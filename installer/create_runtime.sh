#!/usr/bin/bash

# This script creates a custom Java runtime image for Pixelitor using jlink

# JAVA_HOME must point to a JDK 15+
if [ -z "$JAVA_HOME" ]
then
      echo "JAVA_HOME is not set"
	  exit 1
fi	  

if [ ! -d "$JAVA_HOME" ]
then
	echo "JAVA_HOME $JAVA_HOME doesn't exist"
    exit 1
fi

UNAME_OUT="$(uname -s)"
if [[ $UNAME_OUT == CYGWIN* ]]
then
	echo "running on Cygwin"
	CYG_JAVA_HOME=$(cygpath "$JAVA_HOME")	
	JLINK="${CYG_JAVA_HOME}/bin/jlink.exe"
elif [[ $UNAME_OUT == Linux* ]]	
then
	echo "running on Linux"
	JLINK="${JAVA_HOME}/bin/jlink"
elif [[ $UNAME_OUT == Darwin* ]]	
then
	echo "running on Mac"
	JLINK="${JAVA_HOME}/bin/jlink"
else
	echo "Unexpected OS $UNAME_OUT"
	exit 1
fi

echo "JLINK = $JLINK"
if [ ! -f "$JLINK" ]
then
	echo "JLINK $JLINK not found"
    exit 1
fi

"$JLINK" --add-modules java.base,java.desktop,java.datatransfer,java.logging,java.prefs,java.xml --no-header-files --no-man-pages --output runtime

