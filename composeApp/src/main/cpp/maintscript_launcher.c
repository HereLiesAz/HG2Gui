#include <errno.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static int native_dir(char *out, size_t size) {
    ssize_t n = readlink("/proc/self/exe", out, size - 1);
    if (n <= 0 || (size_t)n >= size) return -1;
    out[n] = '\0';
    char *slash = strrchr(out, '/');
    if (!slash) return -1;
    *slash = '\0';
    return 0;
}

static const char *base_name(const char *path) {
    const char *slash = strrchr(path, '/');
    return slash ? slash + 1 : path;
}

int main(int argc, char **argv) {
    const char *prefix = getenv("PREFIX");
    if (!prefix || !*prefix) {
        fprintf(stderr, "hg2gui-maintscript: PREFIX is not set\n");
        return 127;
    }
    if (argc < 1 || !argv[0] || !*argv[0]) {
        fprintf(stderr, "hg2gui-maintscript: missing script identity\n");
        return 127;
    }

    const char *identity = base_name(argv[0]);
    if (!strchr(identity, '.')) {
        fprintf(stderr, "hg2gui-maintscript: invalid script identity '%s'\n", identity);
        return 127;
    }

    char script[PATH_MAX];
    if (snprintf(
            script,
            sizeof(script),
            "%s/var/lib/dpkg/info/%s.hg2body",
            prefix,
            identity
        ) >= (int)sizeof(script)) {
        fprintf(stderr, "hg2gui-maintscript: backing script path is too long\n");
        return 127;
    }
    if (access(script, R_OK) != 0) {
        fprintf(stderr, "hg2gui-maintscript: backing script is unavailable: %s\n", script);
        return 127;
    }

    char dir[PATH_MAX];
    if (native_dir(dir, sizeof(dir)) != 0) {
        fprintf(stderr, "hg2gui-maintscript: cannot resolve native library directory\n");
        return 127;
    }

    char bash[PATH_MAX];
    if (snprintf(bash, sizeof(bash), "%s/libbin_bash.so", dir) >= (int)sizeof(bash)) {
        fprintf(stderr, "hg2gui-maintscript: bash path is too long\n");
        return 127;
    }

    char **next = calloc((size_t)argc + 2, sizeof(char *));
    if (!next) {
        fprintf(stderr, "hg2gui-maintscript: out of memory\n");
        return 127;
    }
    next[0] = bash;
    next[1] = script;
    for (int i = 1; i < argc; ++i) next[i + 1] = argv[i];
    next[argc + 1] = NULL;

    execv(bash, next);
    fprintf(stderr, "hg2gui-maintscript: execv(%s) failed: %s\n", bash, strerror(errno));
    return 127;
}
