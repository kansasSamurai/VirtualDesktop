#!/bin/bash
#
# VirtualDesktop Launch Script for Unix/Linux/macOS
#
# This script launches VirtualDesktop with the appropriate classpath
# and JVM options.

# Determine the installation directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_HOME="$(dirname "$SCRIPT_DIR")"

# Build classpath from lib directory
CLASSPATH=""
for jar in "$APP_HOME"/lib/*.jar; do
    if [ -z "$CLASSPATH" ]; then
        CLASSPATH="$jar"
    else
        CLASSPATH="$CLASSPATH:$jar"
    fi
done

# JVM options
JAVA_OPTS="-Xmx512m"

# Detect Java version and add module options if needed
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -ge 9 ] 2>/dev/null; then
    JAVA_OPTS="$JAVA_OPTS --add-opens=java.base/jdk.internal.loader=ALL-UNNAMED"
fi

# Launch the application
exec java $JAVA_OPTS -cp "$CLASSPATH" org.jwellman.virtualdesktop.App "$@"
