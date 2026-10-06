#!/bin/sh
# Gradle start script (simplified for CI; downloads wrapper jar if needed)
DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"
PROPS="$DIR/gradle/wrapper/gradle-wrapper.properties"
if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Downloading gradle-wrapper.jar..."
  mkdir -p "$DIR/gradle/wrapper"
  curl -fsSL -o "$WRAPPER_JAR" \
    "https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar" \
  || curl -fsSL -o "$WRAPPER_JAR" \
    "https://github.com/gradle/gradle/raw/v8.9.0/gradle/wrapper/gradle-wrapper.jar"
fi
exec java -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
