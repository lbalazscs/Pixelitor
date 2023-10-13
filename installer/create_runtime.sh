#!/usr/bin/bash

# This script creates a custom Java runtime image for Pixelitor using jlink

function print_help {
    echo "Usage: $0 [options]"
    echo "Options:"
    echo "  --dry    Perform a dry run"
    echo "  --help   Show this help message"
}

while [[ $# -gt 0 ]]; do
    key="$1"

    case $key in
    --help)
        print_help
        exit 0
        ;;
    --dry)
        DRY_RUN=1
        shift
        ;;
    *) # unknown option
        shift
        ;;
    esac
done


# JAVA_HOME must point to a JDK 21+
if [ -z "$JAVA_HOME" ]; then
    echo "JAVA_HOME is not set"
    exit 1
fi

if [ ! -d "$JAVA_HOME" ]; then
    echo "JAVA_HOME $JAVA_HOME doesn't exist"
    exit 1
fi
echo "JAVA_HOME is ${JAVA_HOME}"

UNAME_OUT="$(uname -s)"
if [[ $UNAME_OUT == CYGWIN* ]]; then
    echo "running on Cygwin"
    CYG_JAVA_HOME=$(cygpath "$JAVA_HOME")
    JLINK="${CYG_JAVA_HOME}/bin/jlink.exe"
elif [[ $UNAME_OUT == Linux* ]]; then
    echo "running on Linux"
    JLINK="${JAVA_HOME}/bin/jlink"
elif [[ $UNAME_OUT == Darwin* ]]; then
    echo "running on Mac"
    JLINK="${JAVA_HOME}/bin/jlink"
else
    echo "Unexpected OS $UNAME_OUT"
    exit 1
fi

echo "JLINK is $JLINK"
if [ ! -f "$JLINK" ]; then
    echo "JLINK $JLINK not found"
    exit 1
fi

if [[ $DRY_RUN -eq 1 ]]; then
    exit 0
fi
"$JLINK" --add-modules java.base,java.desktop,java.datatransfer,java.logging,java.prefs,java.xml --no-header-files --no-man-pages --output runtime
