#!/bin/bash

 # DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 #
 # Copyright 2012-2013 Oracle and/or its affiliates. All rights reserved.
 #
 # Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 # Other names may be trademarks of their respective owners.
 #
 # The contents of this file are subject to the terms of either the GNU
 # General Public License Version 2 only ("GPL") or the Common
 # Development and Distribution License("CDDL") (collectively, the
 # "License"). You may not use this file except in compliance with the
 # License. You can obtain a copy of the License at
 # http://www.netbeans.org/cddl-gplv2.html
 # or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 # specific language governing permissions and limitations under the
 # License.  When distributing the software, include this License Header
 # Notice in each file and include the License file at
 # nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 # particular file as subject to the "Classpath" exception as provided
 # by Oracle in the GPL Version 2 section of the License file that
 # accompanied this code. If applicable, add the following below the
 # License Header, with the fields enclosed by brackets [] replaced by
 # your own identifying information:
 # "Portions Copyrighted [year] [name of copyright owner]"
 #
 # If you wish your version of this file to be governed by only the CDDL
 # or only the GPL Version 2, indicate your decision by adding
 # "[Contributor] elects to include this software in this distribution
 # under the [CDDL or GPL Version 2] license." If you do not indicate a
 # single choice of license, a recipient has the option to distribute
 # your version of this file under either the CDDL, the GPL Version 2 or
 # to extend the choice of license to its licensees as provided above.
 # However, if you add GPL Version 2 code and therefore, elected the GPL
 # Version 2 license, then the option applies only if the new code is
 # made subject to such option by the copyright holder.
 #
 # Contributor(s):
 #
 # Portions Copyrighted 2012 Sun Microsystems, Inc.

set -x

#Initialize all the environment

#Create test result xml file - call:
#create_test_result( testname, message, failures=0 )
create_test_result() {
    if [ -z "$3" ]; then
        FAILURES="0"
    else
        FAILURES="$3"
    fi

    mkdir -p $WORKSPACE/results

    FILE="$WORKSPACE/results/TEST-$1.xml"
    echo '<?xml version="1.0" encoding="UTF-8" ?>' >$FILE
    echo '<testsuite errors="0" failures="'$FAILURES'" name="'$1'" tests="1" time="1">' >>$FILE
    echo '  <properties>' >>$FILE
    echo '  </properties>' >>$FILE
    echo '  <testcase classname="'$1'" name="'$1'" time="1">' >>$FILE
    if [ "$FAILURES" -gt "0" ]; then
        echo '  <failure message="Failed"/>' >>$FILE
    fi
    echo '  </testcase>' >>$FILE
    echo '  <system-out><![CDATA[' >>$FILE
    echo "$2" >>$FILE
    echo ']]></system-out>' >>$FILE
    echo '  <system-err></system-err>' >>$FILE
    echo '</testsuite>' >>$FILE
}

#NB_BRANCH default
if [ -z ${NB_BRANCH} ]; then
    export NB_BRANCH=default
fi

#L10N_BRANCH default
if [ -z ${L10N_BRANCH} ]; then
    export L10N_BRANCH=default
fi

#OTHER_LICENCES_BRANCH default
if [ -z ${OTHER_LICENCES_BRANCH} ]; then
    export OTHER_LICENCES_BRANCH=default
fi

#JAVAFX build 1/0
if [ -z ${RUNJAVAFX} ]; then
    export RUNJAVAFX=0
fi

#ML_BUILD yes/no 1/0
if [ -z ${ML_BUILD} ]; then
    export ML_BUILD=0
fi
#EN_BUILD yes/no 1/0
if [ -z ${EN_BUILD} ]; then
    export EN_BUILD=1
fi
if [ -z ${LOCALES} ]; then
    export LOCALES=ja,zh_CN,pt_BR,ru
fi

if [ -z ${UPLOAD_ML} ]; then
    export UPLOAD_ML=0
fi

#GLASSFISH_BUILDS_HOST=http://jre.us.oracle.com
if [ -z ${GLASSFISH_BUILDS_HOST} ]; then
    GLASSFISH_BUILDS_HOST=http://jre.us.oracle.com
    export GLASSFISH_BUILDS_HOST
fi

#JDK_BUILDS_HOST=http://jre.us.oracle.com
if [ -z ${JDK_BUILDS_HOST} ]; then
    JDK_BUILDS_HOST=http://jre.us.oracle.com
    export JDK_BUILDS_HOST
fi

#JDK_BUILDS_HOST=http://jre.us.oracle.com
if [ -z ${JRE_BUILDS_PATH} ]; then
    JRE_BUILDS_PATH=java/re/jdk/8u60/promoted/
    export JRE_BUILDS_PATH
fi

#JDK7_BUILDS_PATH=http://jre.us.oracle.com/java/re/jdk/7u75/promoted/all
if [ -z ${JDK7_BUILDS_PATH} ]; then
    JDK7_BUILDS_PATH=java/re/jdk/7u75/promoted/
    export JDK7_BUILDS_PATH
fi

#JDK8_BUILDS_PATH=http://jre.us.oracle.com/java/re/jdk/8u65/promoted/all/
if [ -z ${JDK8_BUILDS_PATH} ]; then
    JDK8_BUILDS_PATH=java/re/jdk/8u65/promoted/
    export JDK8_BUILDS_PATH
fi

if [ -z ${DEBUGLEVEL} ]; then
    DEBUGLEVEL=source,lines,vars
    export DEBUGLEVEL
fi

if [ -z ${DONT_PACK_LOCALIZATION_JARS_ON_MAC} ]; then
    DONT_PACK_LOCALIZATION_JARS_ON_MAC=y
    export DONT_PACK_LOCALIZATION_JARS_ON_MAC
fi

export ANT_OPTS=$ANT_OPTS" -Xmx2G -XX:MaxPermSize=500m"

if [ -n ${JDK_HOME} ] && [ -z ${JAVA_HOME} ] ; then
    export JAVA_HOME=$JDK_HOME
elif [ -n ${JAVA_HOME} ] && [ -z ${JDK_HOME} ]; then
    export JDK_HOME=$JAVA_HOME
fi

if [ -z ${DATESTAMP} ]; then
    if [ -z ${BUILD_ID} ]; then
        export DATESTAMP=`date -u +%Y%m%d%H%M`
    else
        #Use BUILD_ID from hudson, remove all "-" and "_" and cut it to 12 chars
        export DATESTAMP=`echo ${BUILD_ID} | sed -e "s/[-_]//g" | cut -c 1-12`
    fi
fi

BUILDNUM=$BUILD_DESC-$DATESTAMP
BUILDNUMBER=$DATESTAMP

if [ -z $BASE_DIR ]; then
    echo BASE_DIR variable not defined, using the default one: /space/NB-IDE
    echo if you want to use another base dir for the whole build feel free
    echo to define a BASE_DIR variable in your environment
    
    export BASE_DIR=/space/NB-IDE
fi

if [ -z $NB_ALL ]; then
    NB_ALL=$BASE_DIR/main
fi

DIST=$BASE_DIR/dist
LOGS=$DIST/logs
BASENAME=netbeans-$BUILDNUM
export BASENAME_PREFIX=netbeans-$BUILD_DESC

mkdir -p $DIST/zip
mkdir -p $LOGS

#LOGS
IDE_BUILD_LOG=$LOGS/$BASENAME-build-ide.log
MOBILITY_BUILD_LOG=$LOGS/$BASENAME-build-mobility.log
NBMS_BUILD_LOC=$LOGS/$BASENAME-build-nbms.log
SCP_LOG=$LOGS/$BASENAME-scp.log
MAC_LOG=$LOGS/$BASENAME-native_mac.log
MAC_LOG_NEW=$LOGS/$BASENAME-native_mac_new.log
INSTALLER_LOG=$LOGS/$BASENAME-installers.log
