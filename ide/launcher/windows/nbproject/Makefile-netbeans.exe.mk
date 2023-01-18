#
# Generated Makefile - do not edit!
#
# Edit the Makefile in the project folder instead (../Makefile). Each target
# has a -pre and a -post target defined where you can add customized code.
#
# This makefile implements configuration specific macros and targets.


# Environment
MKDIR=mkdir
CP=cp
GREP=grep
NM=nm
CCADMIN=CCadmin
RANLIB=ranlib
CC=gcc
CCC=g++
CXX=g++
FC=gfortran
AS=as

# Macros
CND_PLATFORM=Cygwin-Windows
CND_DLIB_EXT=dll
CND_CONF=netbeans.exe
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/_ext/1413142467/utilsfuncs.o \
	${OBJECTDIR}/nblauncher.o \
	${OBJECTDIR}/netbeans.o


# C Compiler Flags
CFLAGS=

# CC Compiler Flags
CCFLAGS=-m32 -mno-cygwin
CXXFLAGS=-m32 -mno-cygwin

# Fortran Compiler Flags
FFLAGS=

# Assembler Flags
ASFLAGS=

# Link Libraries and Options
LDLIBSOPTIONS=netbeans.res

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk netbeans.exe

netbeans.exe: ${OBJECTFILES}
	${LINK.cc} -o netbeans.exe ${OBJECTFILES} ${LDLIBSOPTIONS} -mwindows -Wl,--nxcompat -Wl,--dynamicbase -Wl,--no-seh

${OBJECTDIR}/_ext/1413142467/utilsfuncs.o: ../../../o.n.bootstrap/launcher/windows/utilsfuncs.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1413142467
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -s -DARCHITECTURE=32 -DNBEXEC_DLL=\"/lib/nbexec.dll\" -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1413142467/utilsfuncs.o ../../../o.n.bootstrap/launcher/windows/utilsfuncs.cpp

${OBJECTDIR}/nblauncher.o: nblauncher.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -s -DARCHITECTURE=32 -DNBEXEC_DLL=\"/lib/nbexec.dll\" -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/nblauncher.o nblauncher.cpp

${OBJECTDIR}/netbeans.o: netbeans.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -s -DARCHITECTURE=32 -DNBEXEC_DLL=\"/lib/nbexec.dll\" -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/netbeans.o netbeans.cpp

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} netbeans.exe

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
