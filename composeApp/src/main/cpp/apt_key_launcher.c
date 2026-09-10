#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

/*
 * Android 10+ forbids execve() of files created in an app's writable data
 * directory. Termux's apt-key is a shell script in $PREFIX/bin, so apt cannot
 * exec it directly even though bash can source/run it. This tiny APK-installed
 * native executable lives in nativeLibraryDir (an exec-allowed location) and
 * turns apt's apt-key invocation into:
 *
 *   $PREFIX/bin/bash $PREFIX/bin/apt-key <original arguments...>
 */
int main(int argc, char **argv) {
    const char *prefix = getenv("PREFIX");
    if (prefix == NULL || prefix[0] == '\0') {
        fprintf(stderr, "hg2gui apt-key launcher: PREFIX is not set\n");
        return 126;
    }

    size_t prefix_len = strlen(prefix);
    char *bash = malloc(prefix_len + sizeof("/bin/bash"));
    char *script = malloc(prefix_len + sizeof("/bin/apt-key"));
    char **child_argv = calloc((size_t)argc + 2, sizeof(char *));
    if (bash == NULL || script == NULL || child_argv == NULL) {
        fprintf(stderr, "hg2gui apt-key launcher: out of memory\n");
        free(bash);
        free(script);
        free(child_argv);
        return 126;
    }

    sprintf(bash, "%s/bin/bash", prefix);
    sprintf(script, "%s/bin/apt-key", prefix);
    child_argv[0] = bash;
    child_argv[1] = script;
    for (int i = 1; i < argc; ++i) child_argv[i + 1] = argv[i];
    child_argv[argc + 1] = NULL;

    execv(bash, child_argv);
    fprintf(stderr, "hg2gui apt-key launcher: execv(%s) failed: %s\n", bash, strerror(errno));
    return 126;
}
