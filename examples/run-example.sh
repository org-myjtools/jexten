#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

RESOURCES_DIR="jexten-example-app/src/main/resources"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  JExten Example Pipeline${NC}"
echo -e "${BLUE}========================================${NC}"

# Step 1: Build and install Maven modules
echo -e "\n${YELLOW}[1/3] Building and installing Maven modules...${NC}"
./mvnw clean install -DskipTests -q

# Step 2: Copy bundles to app resources
echo -e "\n${YELLOW}[2/3] Copying plugin bundles to app resources...${NC}"

# Maven plugins
for plugin in jexten-example-plugin-a jexten-example-plugin-b jexten-example-plugin-c jexten-example-plugin-c1 jexten-example-plugin-c2; do
    BUNDLE=$(find "$plugin/target" -name "*-bundle-*.zip" 2>/dev/null | head -1)
    if [ -n "$BUNDLE" ]; then
        cp "$BUNDLE" "$RESOURCES_DIR/"
        echo -e "  ${GREEN}✓${NC} Copied $(basename "$BUNDLE")"
    else
        echo -e "  ${YELLOW}⚠${NC} Bundle not found for $plugin"
    fi
done

# Step 3: Run the example app
echo -e "\n${YELLOW}[3/3] Running example application...${NC}"
echo -e "${BLUE}----------------------------------------${NC}\n"

# Get classpath from Maven and run
CLASSPATH=$(./mvnw -f jexten-example-app/pom.xml dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q 2>/dev/null)
CLASSPATH="jexten-example-app/target/classes:$CLASSPATH"

java -cp "$CLASSPATH" org.myjtools.jexten.example.app.Main

echo -e "\n${BLUE}----------------------------------------${NC}"
echo -e "${GREEN}Pipeline completed successfully!${NC}"
