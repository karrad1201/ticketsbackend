#!/bin/sh
# Maven Wrapper script
MAVEN_WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"
MAVEN_PROJECTBASEDIR=$(cd "$(dirname "$0")" && pwd)
MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

# Read distributionUrl from properties
DISTRIBUTION_URL=$(grep "^distributionUrl=" "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2-)

# Download directory
MAVEN_USER_HOME="${MAVEN_USER_HOME:-$HOME/.m2}"
MAVEN_DIST_DIR="$MAVEN_USER_HOME/wrapper/dists"

# Parse Maven version from URL
MAVEN_VERSION=$(echo "$DISTRIBUTION_URL" | sed 's/.*apache-maven-\([0-9.]*\)-bin.zip/\1/')
MAVEN_HOME="$MAVEN_DIST_DIR/apache-maven-$MAVEN_VERSION"

if [ ! -d "$MAVEN_HOME" ]; then
    echo "Downloading Apache Maven $MAVEN_VERSION..."
    mkdir -p "$MAVEN_DIST_DIR"
    TEMP_FILE=$(mktemp)
    curl -fsSL "$DISTRIBUTION_URL" -o "$TEMP_FILE"
    unzip -q "$TEMP_FILE" -d "$MAVEN_DIST_DIR"
    rm "$TEMP_FILE"
    echo "Maven $MAVEN_VERSION downloaded."
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
