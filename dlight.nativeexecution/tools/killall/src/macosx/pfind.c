#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <strings.h>
#include "../pfind.h"

#define LINE_LEN 4048

pid_t* pfind(const char* magicenv) {
    FILE* fp;

    if ((fp = popen("/bin/ps -AEww", "r")) == NULL) {
        return NULL;
    }

    char env[strlen(magicenv) + 2];
    sprintf(env, " %s ", magicenv);

    pid_t *result = NULL;
    int res = 0, ressize = 0;
    char line[LINE_LEN];
    char* data;

    pid_t myself = getpid();

    while (fgets(line, LINE_LEN, fp) != 0) {
        line[strlen(line) - 1] = '\0';
        pid_t pid = (pid_t) strtol(line, &data, 0);
        if (pid <= 0 || pid == myself) {
            continue;
        }
        if (strstr(data, env) != 0) {
            if (res >= ressize) {
                ressize += 10;
                result = realloc(result, ressize * sizeof (pid_t));
            }
            result[res++] = pid;
        }
    }
    pclose(fp);

    if (res >= ressize) {
        ressize++;
        result = realloc(result, ressize * sizeof (pid_t));
    }
    result[res] = 0;

    return result;
}
