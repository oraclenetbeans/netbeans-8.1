#!/usr/bin/bash

# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
# Contributor(s):
#
# The Original Software is NetBeans. The Initial Developer of the Original
# Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
# Microsystems, Inc. All Rights Reserved.
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

BuildForJDK()
{
        JAVA_HOME=$1
        JDK_ID=$2
        echo $JAVA_HOME $JDK_ID
	CC_FLAGS="-I$JAVA_HOME/include -I$JAVA_HOME/include/solaris -DSOLARIS -G -lrt \
	-xO2 -v -mt -m64 -KPIC -xc99=%none -xCC -Xa -xstrconst -errwarn=%all"

	cc $CC_FLAGS $PROC_FLAGS \
	-o ../../release/lib/deployed/$JDK_ID/solaris-$PROC/libprofilerinterface.so \
	../src-jdk15/class_file_cache.c \
	../src-jdk15/attach.c \
	../src-jdk15/Classes.c \
	../src-jdk15/HeapDump.c \
	../src-jdk15/Timers.c \
	../src-jdk15/GC.c \
	../src-jdk15/Threads.c \
	../src-jdk15/Stacks.c \
	../src-jdk15/common_functions.c

	rm -f *.o
}

export PROC=`uname -p`
echo PROC is $PROC

if [ $PROC = "i386" ]; then 
  export PROC_FLAGS="-xregs=no%frameptr"
  PROC="amd64"
elif [ $PROC = "sparc" ]; then
  export PROC_FLAGS="-xregs=no%appl"
  PROC="sparcv9"
else 
  echo "Invalid architecture " $PROC
fi

BuildForJDK "$JAVA_HOME_15" "jdk15"
BuildForJDK "$JAVA_HOME_16" "jdk16"

