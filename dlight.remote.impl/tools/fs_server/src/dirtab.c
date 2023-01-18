/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

#include "fs_common.h"
#include "dirtab.h"
#include "util.h"

#include "exitcodes.h"
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <errno.h>
#include <ctype.h>
#include <assert.h>

static char* root = NULL;
static char* temp_path = NULL;
static char* cache_path = NULL;
static char* dirtab_file_path = NULL;
static const char* cache_subdir_name = "cache";

struct dirtab_element {
    int index;
    char* cache_path;
    pthread_mutex_t mutex;
    dirtab_state state;
    dirtab_watch_state watch_state;
    dirtab_refresh_state refresh_state;
    char abspath[];
};

/**guarder by dirtab_mutex */
typedef struct dirtab {

    bool dirty;
    
    /** guards all data in the structure */
    pthread_mutex_t mutex;
    
    /** directories count */
    int size;

    /** max amount of paths before realloc */
    int limit;
    
    /** 
     * List of directories under control
     * sorted in alphabetical order
     */
    dirtab_element** paths;
    
    /** the next unoccupied index */
    int next_index;
} dirtab;

static dirtab table;
static bool initialized = false;
char *persistence_dir = NULL;

void dirtab_set_persistence_dir(const char* dir) {
    if (initialized) {
        report_error("persistence dir should be set BEFORE initialization!\n");
        exit(DIRTAB_SET_PERSIST_DIR_AFTER_INITIALIZATION);
    }
    persistence_dir = strdup(dir);
}

static void init_table() {
    if (initialized) {
        report_error("directories table should only be initialized once!\n");
        exit(DIRTAB_DOUBLE_INITIALIZATION);
    }
    initialized = true;
    pthread_mutex_init(&table.mutex, NULL);
    table.dirty =  false;
    table.size =  0;
    table.limit = 1024;
    table.paths = malloc(table.limit * (sizeof(dirtab_element*)));
    table.next_index = 0;
}

static void expand_table_if_needed() {
    if (table.limit <= table.size) {
        table.limit *= 2;
        table.paths = realloc(table.paths, table.limit * (sizeof(dirtab_element*)));
        if (!table.paths) {
            exit(NO_MEMORY_EXPANDING_DIRTAB);
        }
    }    
}

static dirtab_element *new_dirtab_element(const char* path, int index) {
    char cache[32];
    sprintf(cache, "%s/%d", cache_subdir_name, index);
    int path_len = strlen(path);
    int cache_len = strlen(cache);
    int size = sizeof(dirtab_element) + path_len + cache_len + 2;
    dirtab_element *el = malloc(size);
    el->index = index;
    el->state = DE_STATE_INITIAL;
    el->refresh_state = DRS_NONE;
    el->watch_state = DE_WSTATE_NONE;
    strcpy(el->abspath, path);
    el->cache_path = el->abspath + path_len + 1;
    strcpy(el->cache_path, cache);
    pthread_mutex_init(&el->mutex, NULL);
    return el;
}

static bool load_impl(dirtab_watch_state default_watch_state) {
    FILE *f = fopen(dirtab_file_path, "r");
    if (!f) {
        // TODO: should we really report an error? what if this is just 1-st launch?
        // TODO: if dirtab does not exist, remove all caches
        report_error("error opening %s: %s\n", dirtab_file_path, strerror(errno));
        return false;
    }
    int max_line = PATH_MAX + 40;
    char *line = malloc(max_line);
    table.size = 0;
    table.next_index = 0;
    while (fgets(line, max_line, f)) {
        int index = 0;
        char* p = line;
        while (isdigit(*p)) {
            index *= 10;
            index += (*p) - '0';
            p++;
        }
        if (*p != ' ') {
            report_error("error in file %s: index not followed by space in line '%s'\n", dirtab_file_path, line);
            fclose(f);
            return false; //TODO: clear the table!
        }
        char* path = ++p;
        // cut off '\n\ before trailing '\0'
        while (*p) {
            p++;
        }        
        // p points to trailing '\0'
        if (p >= path) {
            p--;
        }
        if (*p == '\n') {
            *p = 0;
        }
        unescape_strcpy(path, path);
        expand_table_if_needed();
        table.paths[table.size] = new_dirtab_element(path, index);
        table.paths[table.size]->watch_state = default_watch_state;
        table.size++;
        if (index + 1 > table.next_index) {
            table.next_index = index + 1;
        }
    }
    free(line);
    if (fclose(f) == 0) {
        return true;
    } else {
        report_error("error closing %s: %s\n", dirtab_file_path, strerror(errno));
        return false;
    }
}

static bool load_table(dirtab_watch_state default_watch_state) {
    if (!file_exists(dirtab_file_path)) {
        return false;
    }
    mutex_lock(&table.mutex);
    bool result = load_impl(default_watch_state);
    mutex_unlock(&table.mutex);
    return result;    
}

static bool flush_impl() {
    FILE *fp = fopen600(dirtab_file_path);
    if (!fp){
        report_error("error opening %s for writing: %s\n", dirtab_file_path, strerror(errno));
        return false;
    }
    int i;
    char* buf = malloc(PATH_MAX * 2); 
    for (i = 0; i < table.size; i++) {
        if (table.paths[i]->state != DE_STATE_REMOVED) {
            escape_strcpy(buf, table.paths[i]->abspath);
            fprintf(fp, "%d %s\n", table.paths[i]->index, buf);
        }
    }
    free(buf);
    if (fclose(fp) == 0) {
        return true;
    } else {
        report_error("error closing %s for writing: %s\n", dirtab_file_path, strerror(errno));
        return false;
    }
}

/** call dirtab_lock() before!  */
dirtab_state dirtab_get_state(dirtab_element *el) {
    return el->state;
}

/** call dirtab_lock() before!  */
dirtab_refresh_state dirtab_get_refresh_state(dirtab_element *el) {
    return el->refresh_state;
}

/** call dirtab_lock() before!  */
void dirtab_set_refresh_state(dirtab_element *el, dirtab_refresh_state state) {
    el->refresh_state = state;
}

/** call dirtab_lock() before!  */
void dirtab_set_state(dirtab_element *el, dirtab_state state) {
    el->state = state;
}

bool dirtab_flush() {
    mutex_lock(&table.mutex);
    bool result;
    if (table.dirty) {
        result = flush_impl();
        table.dirty = false;
    } else {
        result = true;
    }
    mutex_unlock(&table.mutex);
    return result;
}

const char* dirtab_get_tempdir() {
    return temp_path;
}

const char* dirtab_get_basedir() {
    return root;
}

static void mkdir_or_die(const char *path, int exit_code_fail_create, int exit_code_fail_access) {
    struct stat stat_buf;
    if (stat(path, &stat_buf) == -1) {
        if (errno == ENOENT) {
            if (mkdir(path, 0700) != 0) {
                report_error("error creating directory '%s': %s\n", path, strerror(errno));
                exit(exit_code_fail_create);
            }
        } else {
            report_error("error accessing directory '%s': %s\n", path, strerror(errno));
            exit(exit_code_fail_access);
        }
    } else if(!S_ISDIR(stat_buf.st_mode)) {
        report_error("error accessing directory '%s': not a directory\n", path, strerror(errno));
        exit(exit_code_fail_access);        
    }
}

/** recursive mkdir_or_die */
static void mkdir_or_die_recursive(const char *path, int exit_code_fail_create, int exit_code_fail_access) {
    char* buf = malloc(PATH_MAX+1);
    strncpy(buf, path, PATH_MAX);
    char* slash = strchr(buf+1, '/');
    while (slash) {
        *slash = 0;
        mkdir_or_die(buf, exit_code_fail_create, exit_code_fail_access);
        *slash = '/';
        slash = strchr(slash+1, '/');
    }
    mkdir_or_die(path, exit_code_fail_create, exit_code_fail_access);
    free(buf);
}

static void fill_default_root() {
    const char* home = get_home_dir();
    if (!home) {
        report_error("can't determine home directory\n");
        exit(FAILURE_GETTING_HOME_DIR);
    }
    strncpy(root, home, PATH_MAX);
    strcat(root, "/.netbeans");
    strcat(root, "/remotefs");
}

void dirtab_init(bool clear_persistence, dirtab_watch_state default_watch_state) {

    root = malloc(PATH_MAX + 1);
    temp_path = malloc(PATH_MAX + 1);
    cache_path = malloc(PATH_MAX + 1);
    dirtab_file_path = malloc(PATH_MAX + 1);

    char* pdir = persistence_dir ? persistence_dir : "0";
    
    int len = strlen(pdir);
    if (*pdir != '/') {
        fill_default_root();
        len += strlen(root) + 1; // +1 is for '/'
    }
    if (len > PATH_MAX) {
        report_error("too long persistence path\n");
        exit(WRONG_ARGUMENT);
    }
    if (*pdir == '/') {
        strcpy(root, pdir);
    } else {
        fill_default_root();
        strcat(root, "/");
        strcat(root, pdir);
    }
    if (persistence_dir) {
        free(persistence_dir);
        persistence_dir = NULL; //just in case
    }

    mkdir_or_die_recursive(root, FAILURE_CREATING_STORAGE_DIR, FAILURE_ACCESSING_STORAGE_DIR);
    
    if (clear_persistence) {
        trace(TRACE_INFO, "Cleaning up persistence (%s)\n", root);
        clean_dir(root);
    }
    
    strcpy(cache_path, root);
    strcat(cache_path, "/");
    strcat(cache_path, cache_subdir_name);
    mkdir_or_die(cache_path, FAILURE_CREATING_CACHE_DIR, FAILURE_ACCESSING_CACHE_DIR);

    strcpy(temp_path, root);
    strcat(temp_path, "/tmp");
    mkdir_or_die(temp_path, FAILURE_CREATING_TEMP_DIR, FAILURE_ACCESSING_TEMP_DIR);

    strcpy(dirtab_file_path, root);
    strcat(dirtab_file_path, "/dirtab");
    
    init_table();
    load_table(default_watch_state);
}

void dirtab_free() {
    mutex_lock(&table.mutex);
    for (int i = 0; i < table.size; i++) {
        free(table.paths[i]);
    }
    table.size = 0;
    free(table.paths);
    mutex_unlock(&table.mutex);
    free(root);
    free(temp_path);
    free(cache_path);
    free(dirtab_file_path);    
    // just in case:
    root = NULL;
    temp_path = NULL;
    cache_path = NULL;
    dirtab_file_path = NULL;
}

dirtab_element *dirtab_get_element(const char* abspath) {

    mutex_lock(&table.mutex);

    dirtab_element *el;

    // perform a binary search
    bool found = false;
        
    int left = 0;
    int right = table.size - 1;
    
    if (table.size != 0) {
        while (left <= right) {
            int x = (left + right) / 2;
            int cmp = strcmp(abspath, table.paths[x]->abspath);
            if (cmp < 0) {
                right = x - 1; 
            } else if (cmp > 0) {
                left  = x + 1; 
            } else {
                found = true;
                el = table.paths[x];
                break;
            }
        }
    }

    if (!found) {
        el = new_dirtab_element(abspath, table.next_index++);
        expand_table_if_needed();
        for (int i = table.size-1; i >= left; i--) { 
            table.paths[i+1] = table.paths[i];
        }
        table.paths[left] = el;
        table.dirty = true;        
        table.size++;
    }

    mutex_unlock(&table.mutex);

    return el;    
}

static void trace_lock_unlock(dirtab_element *el, bool lock) {
    //trace("# %s mutex for %s\n", lock ? "locking" : "unlocking", el->abspath);
}

/** just a wrapper for tracing/logging/debugging */
void dirtab_lock(dirtab_element *el) {
    trace_lock_unlock(el, true);
    mutex_lock(&el->mutex);
}

/** just a wrapper for tracing/logging/debugging */
void dirtab_unlock(dirtab_element *el) {
    trace_lock_unlock(el, false);
    mutex_unlock(&el->mutex);
}

const char*  dirtab_get_element_cache_path(dirtab_element *el) {
    return el->cache_path;
}

void dirtab_visit(bool (*visitor) (const char* path, int index, dirtab_element* el, void *data), void *data) {
    mutex_lock(&table.mutex);
    int size = table.size;
    int mem_size = size * sizeof(dirtab_element**);
    dirtab_element** paths = malloc(mem_size);
    memcpy(paths, table.paths, mem_size);
    mutex_unlock(&table.mutex);
    for (int i = 0; i < size; i++) {
        dirtab_element* el = paths[i];
        bool proceed = visitor(el->abspath, el->index, el, data);
        if (!proceed) {
            break;
        }
    }
    free(paths);
}

bool dirtab_is_empty() {
    mutex_lock(&table.mutex);
    int size = table.size;
    mutex_unlock(&table.mutex);
    return size == 0;
}

/** call dirtab_lock() before!  */
dirtab_watch_state dirtab_get_watch_state(dirtab_element *el) {
    return el->watch_state;
}

/** call dirtab_lock() before!  */
void dirtab_set_watch_state(dirtab_element *el, dirtab_watch_state state) {
    el->watch_state = state;
}
