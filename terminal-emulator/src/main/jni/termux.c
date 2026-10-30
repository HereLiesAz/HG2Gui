#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <jni.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>

#define TERMUX_UNUSED(x) x __attribute__((__unused__))
#ifdef __APPLE__
# define LACKS_PTSNAME_R
#endif

/* Throws a RuntimeException unless an exception is already pending (e.g. OutOfMemoryError from
 * GetStringUTFChars): calling ThrowNew on top of a pending exception is forbidden by JNI. */
static int throw_runtime_exception(JNIEnv* env, char const* message)
{
    if ((*env)->ExceptionCheck(env)) return -1;
    jclass exClass = (*env)->FindClass(env, "java/lang/RuntimeException");
    if (exClass == NULL) return -1; /* NoClassDefFoundError is pending */
    (*env)->ThrowNew(env, exClass, message);
    (*env)->DeleteLocalRef(env, exClass);
    return -1;
}

/* Frees a NULL-terminated array of malloc'ed strings and the array itself. */
static void free_string_array(char** array)
{
    if (!array) return;
    for (char** tmp = array; *tmp; ++tmp) free(*tmp);
    free(array);
}

/* Copies a Java String[] into a calloc'ed, NULL-terminated char* array. On failure nothing is
 * leaked, a Java exception is pending and NULL is returned. */
static char** copy_string_array(JNIEnv* env, jobjectArray array, jsize size, char const* what)
{
    char** copy = (char**) calloc((size_t) size + 1, sizeof(char*));
    if (!copy) {
        throw_runtime_exception(env, "calloc() for string array failed");
        return NULL;
    }
    for (jsize i = 0; i < size; ++i) {
        jstring java_string = (jstring) (*env)->GetObjectArrayElement(env, array, i);
        if (!java_string) {
            /* Either a null element or a pending ArrayIndexOutOfBoundsException. */
            throw_runtime_exception(env, what);
            free_string_array(copy);
            return NULL;
        }
        char const* utf8 = (*env)->GetStringUTFChars(env, java_string, NULL);
        if (!utf8) {
            (*env)->DeleteLocalRef(env, java_string);
            throw_runtime_exception(env, what); /* no-op when OutOfMemoryError is already pending */
            free_string_array(copy);
            return NULL;
        }
        copy[i] = strdup(utf8);
        (*env)->ReleaseStringUTFChars(env, java_string, utf8);
        /* Don't accumulate one local reference per element (the table is small). */
        (*env)->DeleteLocalRef(env, java_string);
        if (!copy[i]) {
            throw_runtime_exception(env, "strdup() failed");
            free_string_array(copy);
            return NULL;
        }
    }
    return copy;
}

static int create_subprocess(JNIEnv* env,
        char const* cmd,
        char const* cwd,
        char* const argv[],
        char** envp,
        int* pProcessId,
        jint rows,
        jint columns,
        jint cell_width,
        jint cell_height)
{
    int ptm = open("/dev/ptmx", O_RDWR | O_CLOEXEC);
    if (ptm < 0) return throw_runtime_exception(env, "Cannot open /dev/ptmx");

#ifdef LACKS_PTSNAME_R
    char* devname;
#else
    char devname[64];
#endif
    if (grantpt(ptm) || unlockpt(ptm) ||
#ifdef LACKS_PTSNAME_R
            (devname = ptsname(ptm)) == NULL
#else
            ptsname_r(ptm, devname, sizeof(devname))
#endif
       ) {
        close(ptm);
        return throw_runtime_exception(env, "Cannot grantpt()/unlockpt()/ptsname_r() on /dev/ptmx");
    }

    // Enable UTF-8 mode and disable flow control to prevent Ctrl+S from locking up the display.
    struct termios tios;
    tcgetattr(ptm, &tios);
    tios.c_iflag |= IUTF8;
    tios.c_iflag &= ~(IXON | IXOFF);
    tcsetattr(ptm, TCSANOW, &tios);

    /** Set initial winsize. */
    struct winsize sz = { .ws_row = (unsigned short) rows, .ws_col = (unsigned short) columns, .ws_xpixel = (unsigned short) (columns * cell_width), .ws_ypixel = (unsigned short) (rows * cell_height)};
    ioctl(ptm, TIOCSWINSZ, &sz);

    pid_t pid = fork();
    if (pid < 0) {
        close(ptm);
        return throw_runtime_exception(env, "Fork failed");
    } else if (pid > 0) {
        *pProcessId = (int) pid;
        return ptm;
    } else {
        // Clear signals which the Android java process may have blocked:
        sigset_t signals_to_unblock;
        sigfillset(&signals_to_unblock);
        sigprocmask(SIG_UNBLOCK, &signals_to_unblock, 0);

        close(ptm);
        setsid();

        int pts = open(devname, O_RDWR);
        if (pts < 0) exit(-1);

        dup2(pts, 0);
        dup2(pts, 1);
        dup2(pts, 2);

        DIR* self_dir = opendir("/proc/self/fd");
        if (self_dir != NULL) {
            int self_dir_fd = dirfd(self_dir);
            struct dirent* entry;
            while ((entry = readdir(self_dir)) != NULL) {
                int fd = atoi(entry->d_name);
                if (fd > 2 && fd != self_dir_fd) close(fd);
            }
            closedir(self_dir);
        }

        clearenv();
        if (envp) for (; *envp; ++envp) putenv(*envp);

        if (chdir(cwd) != 0) {
            char* error_message;
            // No need to free asprintf()-allocated memory since doing execvp() or exit() below.
            if (asprintf(&error_message, "chdir(\"%s\")", cwd) == -1) error_message = "chdir()";
            perror(error_message);
            fflush(stderr);
        }
        execvp(cmd, argv);
        // Show terminal output about failing exec() call:
        char* error_message;
        if (asprintf(&error_message, "exec(\"%s\")", cmd) == -1) error_message = "exec()";
        perror(error_message);
        _exit(1);
    }
}

JNIEXPORT jint JNICALL Java_com_termux_terminal_JNI_createSubprocess(
        JNIEnv* env,
        jclass TERMUX_UNUSED(clazz),
        jstring cmd,
        jstring cwd,
        jobjectArray args,
        jobjectArray envVars,
        jintArray processIdArray,
        jint rows,
        jint columns,
        jint cell_width,
        jint cell_height)
{
    jsize size = args ? (*env)->GetArrayLength(env, args) : 0;
    char** argv = NULL;
    if (size > 0) {
        argv = copy_string_array(env, args, size, "GetStringUTFChars() failed for argv");
        if (!argv) return -1;
    }

    size = envVars ? (*env)->GetArrayLength(env, envVars) : 0;
    char** envp = NULL;
    if (size > 0) {
        envp = copy_string_array(env, envVars, size, "GetStringUTFChars() failed for env");
        if (!envp) {
            free_string_array(argv);
            return -1;
        }
    }

    int procId = 0;
    int ptm = -1;
    char const* cmd_cwd = (*env)->GetStringUTFChars(env, cwd, NULL);
    char const* cmd_utf8 = cmd_cwd ? (*env)->GetStringUTFChars(env, cmd, NULL) : NULL;
    if (cmd_cwd && cmd_utf8) {
        ptm = create_subprocess(env, cmd_utf8, cmd_cwd, argv, envp, &procId, rows, columns, cell_width, cell_height);
    } else {
        throw_runtime_exception(env, "GetStringUTFChars() failed for cmd/cwd");
    }
    /* Release*Chars are among the few JNI calls that are legal with an exception pending. */
    if (cmd_cwd) (*env)->ReleaseStringUTFChars(env, cwd, cmd_cwd);
    if (cmd_utf8) (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf8);

    free_string_array(argv);
    free_string_array(envp);

    /* create_subprocess() throws a Java exception on every failure path; GetPrimitiveArrayCritical
     * must not be called with one pending, so bail out before touching the array. */
    if (ptm < 0 || (*env)->ExceptionCheck(env)) {
        if (ptm >= 0) close(ptm);
        return -1;
    }

    int* pProcId = (int*) (*env)->GetPrimitiveArrayCritical(env, processIdArray, NULL);
    if (!pProcId) {
        close(ptm);
        return throw_runtime_exception(env, "JNI call GetPrimitiveArrayCritical(processIdArray, &isCopy) failed");
    }

    *pProcId = procId;
    (*env)->ReleasePrimitiveArrayCritical(env, processIdArray, pProcId, 0);

    return ptm;
}

JNIEXPORT void JNICALL Java_com_termux_terminal_JNI_setPtyWindowSize(JNIEnv* TERMUX_UNUSED(env), jclass TERMUX_UNUSED(clazz), jint fd, jint rows, jint cols, jint cell_width, jint cell_height)
{
    struct winsize sz = { .ws_row = (unsigned short) rows, .ws_col = (unsigned short) cols, .ws_xpixel = (unsigned short) (cols * cell_width), .ws_ypixel = (unsigned short) (rows * cell_height) };
    ioctl(fd, TIOCSWINSZ, &sz);
}

JNIEXPORT void JNICALL Java_com_termux_terminal_JNI_setPtyUTF8Mode(JNIEnv* TERMUX_UNUSED(env), jclass TERMUX_UNUSED(clazz), jint fd)
{
    struct termios tios;
    tcgetattr(fd, &tios);
    if ((tios.c_iflag & IUTF8) == 0) {
        tios.c_iflag |= IUTF8;
        tcsetattr(fd, TCSANOW, &tios);
    }
}

JNIEXPORT jint JNICALL Java_com_termux_terminal_JNI_waitFor(JNIEnv* TERMUX_UNUSED(env), jclass TERMUX_UNUSED(clazz), jint pid)
{
    int status;
    pid_t waited;
    // Retry when a signal interrupts the wait; any other failure (e.g. ECHILD, the child was
    // already reaped) leaves status uninitialised, so report it as a failed exit instead.
    do {
        waited = waitpid(pid, &status, 0);
    } while (waited < 0 && errno == EINTR);
    if (waited < 0) return -1;
    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    } else if (WIFSIGNALED(status)) {
        return -WTERMSIG(status);
    } else {
        // Should never happen - waitpid(2) says "One of the first three macros will evaluate to a non-zero (true) value".
        return 0;
    }
}

JNIEXPORT void JNICALL Java_com_termux_terminal_JNI_close(JNIEnv* TERMUX_UNUSED(env), jclass TERMUX_UNUSED(clazz), jint fileDescriptor)
{
    close(fileDescriptor);
}
