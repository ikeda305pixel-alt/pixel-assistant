#!/bin/sh

# Gradle start-up script for POSIX systems
APP_BASE_NAME=`basename "$0"`
APP_HOME="`pwd -P`"

# If gradle is installed on PATH (e.g. runner), use it or use wrapper
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi

# Fallback to standard java -jar gradle-wrapper
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec java -Xmx1024m -Dfile.encoding=UTF-8 -jar "$CLASSPATH" "$@"
