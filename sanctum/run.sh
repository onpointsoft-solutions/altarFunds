#!/bin/bash

# Sanctum Church Management System - Run Script
# This script handles building and running the application

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project root directory
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="$PROJECT_DIR/target/sanctum-church-management-1.0.0.jar"

echo -e "${BLUE}🏛️ Sanctum Church Management System${NC}"
echo -e "${BLUE}======================================${NC}"

# Function to check if Java is installed
check_java() {
    if ! command -v java &> /dev/null; then
        echo -e "${RED}❌ Java is not installed or not in PATH${NC}"
        echo "Please install Java 11 or higher to run Sanctum"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 11 ]; then
        echo -e "${RED}❌ Java 11 or higher is required. Current version: $JAVA_VERSION${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ Java version check passed${NC}"
}

# Function to check if Maven is installed
check_maven() {
    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}❌ Maven is not installed or not in PATH${NC}"
        echo "Please install Maven to build Sanctum"
        exit 1
    fi
    
    echo -e "${GREEN}✅ Maven check passed${NC}"
}

# Function to build the project
build_project() {
    echo -e "${YELLOW}🔨 Building Sanctum...${NC}"
    
    cd "$PROJECT_DIR"
    
    # Clean and package
    mvn clean package -DskipTests
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Build successful${NC}"
    else
        echo -e "${RED}❌ Build failed${NC}"
        exit 1
    fi
}

# Function to run the application
run_application() {
    echo -e "${YELLOW}🚀 Starting Sanctum...${NC}"
    
    if [ ! -f "$JAR_FILE" ]; then
        echo -e "${RED}❌ JAR file not found: $JAR_FILE${NC}"
        echo "Building project first..."
        build_project
    fi
    
    # Run the application
    cd "$PROJECT_DIR"
    java -jar "$JAR_FILE"
}

# Function to show help
show_help() {
    echo -e "${BLUE}Sanctum Church Management System - Usage:${NC}"
    echo ""
    echo "  $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  build     - Build the project only"
    echo "  run       - Run the application (builds if needed)"
    echo "  clean     - Clean build artifacts"
    echo "  dev       - Development mode (build and run)"
    echo "  help      - Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 run        # Run the application"
    echo "  $0 build      # Build only"
    echo "  $0 clean      # Clean build files"
    echo "  $0 dev        # Development mode"
}

# Function to clean build artifacts
clean_project() {
    echo -e "${YELLOW}🧹 Cleaning build artifacts...${NC}"
    
    cd "$PROJECT_DIR"
    mvn clean
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Clean successful${NC}"
    else
        echo -e "${RED}❌ Clean failed${NC}"
        exit 1
    fi
}

# Main script logic
case "${1:-run}" in
    "build")
        check_java
        check_maven
        build_project
        ;;
    "run")
        check_java
        run_application
        ;;
    "clean")
        clean_project
        ;;
    "dev")
        check_java
        check_maven
        build_project
        run_application
        ;;
    "help"|"-h"|"--help")
        show_help
        ;;
    *)
        echo -e "${RED}❌ Unknown command: $1${NC}"
        echo ""
        show_help
        exit 1
        ;;
esac

echo -e "${GREEN}🙏 Thank you for using Sanctum Church Management System!${NC}"
