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

int main(int argc, char **argv) {
    const char *prefix = getenv("PREFIX");
    if (!prefix || !*prefix) {
        fprintf(stderr, "hg2gui-dpkg: PREFIX is not set\n");
        return 127;
    }

    char dir[PATH_MAX];
    if (native_dir(dir, sizeof(dir)) != 0) {
        fprintf(stderr, "hg2gui-dpkg: cannot resolve native library directory\n");
        return 127;
    }

    char target[PATH_MAX];
    if (snprintf(target, sizeof(target), "%s/libbin_dpkg.so", dir) >= (int)sizeof(target)) {
        fprintf(stderr, "hg2gui-dpkg: dpkg path is too long\n");
        return 127;
    }

    char admindir[PATH_MAX];
    if (snprintf(admindir, sizeof(admindir), "--admindir=%s/var/lib/dpkg", prefix) >= (int)sizeof(admindir)) {
        fprintf(stderr, "hg2gui-dpkg: admindir path is too long\n");
        return 127;
    }

    char instdir[PATH_MAX];
    if (snprintf(instdir, sizeof(instdir), "--instdir=%s", prefix) >= (int)sizeof(instdir)) {
        fprintf(stderr, "hg2gui-dpkg: instdir path is too long\n");
        return 127;
    }

    char **next = calloc((size_t)argc + 3, sizeof(char *));
    if (!next) {
        fprintf(stderr, "hg2gui-dpkg: out of memory\n");
        return 127;
    }

    next[0] = target;
    next[1] = admindir;
    next[2] = instdir;
    for (int i = 1; i < argc; ++i) next[i + 2] = argv[i];
    next[argc + 2] = NULL;

    execv(target, next);
    fprintf(stderr, "hg2gui-dpkg: execv(%s) failed: %s\n", target, strerror(errno));
    return 127;
}
