#define _GNU_SOURCE
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stdlib.h>
#include <string.h>
#include <sys/syscall.h>
#include <unistd.h>

extern char **environ;

static int starts_with_path(const char *path, const char *root) {
    if (!path || !root || !*root) return 0;
    size_t n = strlen(root);
    if (strncmp(path, root, n) != 0) return 0;
    return path[n] == '\0' || path[n] == '/';
}

static int hg2gui_owned_path(const char *path) {
    const char *prefix = getenv("PREFIX");
    const char *home = getenv("HOME");
    return starts_with_path(path, prefix) || starts_with_path(path, home);
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

__attribute__((visibility("default")))
int execve(const char *pathname, char *const argv[], char *const envp[]) {
    if (!pathname || !*pathname) {
        errno = ENOENT;
        return -1;
    }

    char resolved[PATH_MAX];
    const char *candidate = pathname;
    if (realpath(pathname, resolved) != NULL) candidate = resolved;

    if (!hg2gui_owned_path(candidate) || !is_elf(candidate)) {
        return raw_execve(pathname, argv, envp);
    }

#if defined(__aarch64__) || defined(__x86_64__)
    const char *linker = "/system/bin/linker64";
#else
    const char *linker = "/system/bin/linker";
#endif

    size_t argc = 0;
    if (argv) while (argv[argc]) argc++;

    char **next = calloc(argc + 2, sizeof(char *));
    if (!next) {
        errno = ENOMEM;
        return -1;
    }

    next[0] = (char *)linker;
    next[1] = (char *)candidate;
    for (size_t i = 1; i < argc; ++i) next[i + 1] = argv[i];
    next[argc + 1] = NULL;

    int result = raw_execve(linker, next, envp ? envp : environ);
    int saved_errno = errno;
    free(next);
    errno = saved_errno;
    return result;
}
