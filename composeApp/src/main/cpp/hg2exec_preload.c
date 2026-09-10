#define _GNU_SOURCE
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/syscall.h>
#include <unistd.h>

extern char **environ;

/* Cached at library load time so every exec intercept doesn't hit getenv() twice. */
static const char *g_prefix;
static const char *g_home;

__attribute__((constructor))
static void hg2exec_init(void) {
    g_prefix = getenv("PREFIX");
    g_home   = getenv("HOME");
}

static int starts_with_path(const char *path, const char *root) {
    if (!path || !root || !*root) return 0;
    size_t n = strlen(root);
    if (strncmp(path, root, n) != 0) return 0;
    return path[n] == '\0' || path[n] == '/';
}

static int hg2gui_owned_path(const char *path) {
    return starts_with_path(path, g_prefix) || starts_with_path(path, g_home);
}

static int is_elf(const char *path) {
    unsigned char magic[4];
    int fd = open(path, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return 0;
    ssize_t n = read(fd, magic, sizeof(magic));
    close(fd);
    return n == (ssize_t)sizeof(magic) &&
           magic[0] == 0x7f && magic[1] == 'E' && magic[2] == 'L' && magic[3] == 'F';
}

static int raw_execve(const char *pathname, char *const argv[], char *const envp[]) {
    return (int)syscall(__NR_execve, pathname, argv, envp);
}

static int hg2_execve(const char *pathname, char *const argv[], char *const envp[]) {
    if (!pathname || !*pathname) {
        errno = ENOENT;
        return -1;
    }

    char resolved[PATH_MAX];
    const char *candidate = pathname;
    if (realpath(pathname, resolved) != NULL) candidate = resolved;

    if (!hg2gui_owned_path(candidate) || !is_elf(candidate)) {
        /* Pass envp through as-is; NULL means caller explicitly cleared the environment. */
        return raw_execve(pathname, argv, envp);
    }

#if defined(__aarch64__) || defined(__x86_64__)
    const char *linker = "/system/bin/linker64";
#else
    const char *linker = "/system/bin/linker";
#endif

    size_t argc = 0;
    if (argv) while (argv[argc]) argc++;

    char **next = calloc(argc + 3, sizeof(char *));
    if (!next) {
        errno = ENOMEM;
        return -1;
    }

    /* argv layout: [linker, candidate, argv[0] (original program name), argv[1..argc-1], NULL]
     * argv[0] was previously dropped; the dynamic linker uses it as the program identity. */
    next[0] = (char *)linker;
    next[1] = (char *)candidate;
    for (size_t i = 0; i < argc; ++i) next[i + 2] = argv[i];
    next[argc + 2] = NULL;

    int result = raw_execve(linker, next, envp);
    int saved_errno = errno;
    free(next);
    errno = saved_errno;
    return result;
}

static int path_exec(const char *name, char *const argv[], char *const envp[]) {
    if (!name || !*name) {
        errno = ENOENT;
        return -1;
    }
    if (strchr(name, '/')) return hg2_execve(name, argv, envp);

    const char *path = getenv("PATH");
    if (!path || !*path) path = "/system/bin:/system/xbin";

    int saw_eacces = 0;
    const char *cursor = path;
    while (1) {
        const char *colon = strchr(cursor, ':');
        size_t dir_len = colon ? (size_t)(colon - cursor) : strlen(cursor);
        const char *dir = cursor;
        if (dir_len == 0) {
            dir = ".";
            dir_len = 1;
        }

        if (dir_len + 1 + strlen(name) + 1 <= PATH_MAX) {
            char candidate[PATH_MAX];
            memcpy(candidate, dir, dir_len);
            candidate[dir_len] = '/';
            strcpy(candidate + dir_len + 1, name);

            hg2_execve(candidate, argv, envp);
            if (errno == EACCES) {
                saw_eacces = 1;
            } else if (errno != ENOENT && errno != ENOTDIR) {
                return -1;
            }
        }

        if (!colon) break;
        cursor = colon + 1;
    }

    errno = saw_eacces ? EACCES : ENOENT;
    return -1;
}

static char **collect_argv(const char *arg, va_list ap, char *const **envp_out, int has_envp) {
    size_t capacity = 8;
    size_t count = 0;
    char **argv = calloc(capacity, sizeof(char *));
    if (!argv) {
        errno = ENOMEM;
        return NULL;
    }

    const char *current = arg;
    while (current) {
        if (count + 1 >= capacity) {
            size_t next_capacity = capacity * 2;
            char **grown = realloc(argv, next_capacity * sizeof(char *));
            if (!grown) {
                int saved_errno = errno;
                free(argv);
                errno = saved_errno ? saved_errno : ENOMEM;
                return NULL;
            }
            argv = grown;
            capacity = next_capacity;
        }
        argv[count++] = (char *)current;
        current = va_arg(ap, const char *);
    }
    argv[count] = NULL;

    if (has_envp && envp_out) {
        *envp_out = va_arg(ap, char *const *);
    }
    return argv;
}

__attribute__((visibility("default")))
int execve(const char *pathname, char *const argv[], char *const envp[]) {
    return hg2_execve(pathname, argv, envp);
}

__attribute__((visibility("default")))
int execv(const char *pathname, char *const argv[]) {
    return hg2_execve(pathname, argv, environ);
}

__attribute__((visibility("default")))
int execvp(const char *name, char *const argv[]) {
    return path_exec(name, argv, environ);
}

__attribute__((visibility("default")))
int execvpe(const char *name, char *const argv[], char *const envp[]) {
    return path_exec(name, argv, envp ? envp : environ);
}

__attribute__((visibility("default")))
int execl(const char *pathname, const char *arg, ...) {
    va_list ap;
    va_start(ap, arg);
    char **argv = collect_argv(arg, ap, NULL, 0);
    va_end(ap);
    if (!argv) return -1;

    int result = hg2_execve(pathname, argv, environ);
    int saved_errno = errno;
    free(argv);
    errno = saved_errno;
    return result;
}

__attribute__((visibility("default")))
int execlp(const char *name, const char *arg, ...) {
    va_list ap;
    va_start(ap, arg);
    char **argv = collect_argv(arg, ap, NULL, 0);
    va_end(ap);
    if (!argv) return -1;

    int result = path_exec(name, argv, environ);
    int saved_errno = errno;
    free(argv);
    errno = saved_errno;
    return result;
}

__attribute__((visibility("default")))
int execle(const char *pathname, const char *arg, ...) {
    va_list ap;
    va_start(ap, arg);
    char *const *supplied_envp = NULL;
    char **argv = collect_argv(arg, ap, &supplied_envp, 1);
    va_end(ap);
    if (!argv) return -1;

    int result = hg2_execve(pathname, argv, supplied_envp ? supplied_envp : environ);
    int saved_errno = errno;
    free(argv);
    errno = saved_errno;
    return result;
}

__attribute__((visibility("default")))
int fexecve(int fd, char *const argv[], char *const envp[]) {
    if (fd < 0) {
        errno = EBADF;
        return -1;
    }

    char proc_path[64];
    char resolved[PATH_MAX];
    int written = snprintf(proc_path, sizeof(proc_path), "/proc/self/fd/%d", fd);
    if (written > 0 && (size_t)written < sizeof(proc_path)) {
        ssize_t length = readlink(proc_path, resolved, sizeof(resolved) - 1);
        if (length > 0) {
            resolved[length] = '\0';
            /* Delegate to hg2_execve which performs its own ownership and ELF checks, avoiding
             * a TOCTOU window between a pre-check here and the re-check inside hg2_execve. */
            if (hg2gui_owned_path(resolved)) {
                return hg2_execve(resolved, argv, envp);
            }
        }
    }

#if defined(__NR_execveat) && defined(AT_EMPTY_PATH)
    return (int)syscall(__NR_execveat, fd, "", argv, envp, AT_EMPTY_PATH);
#else
    errno = ENOSYS;
    return -1;
#endif
}
