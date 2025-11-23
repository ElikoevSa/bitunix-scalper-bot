@echo off
echo Starting Bitunix Scalper Bot (Java 11 Compatible)...
echo.

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 11 or higher
    pause
    exit /b 1
)

REM Check if Maven is installed
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Apache Maven
    pause
    exit /b 1
)

echo Java and Maven are available
echo.

REM Clean and compile the project
echo Compiling project...
mvn clean compile -DskipTests
if %errorlevel% neq 0 (
    echo ERROR: Compilation failed
    pause
    exit /b 1
)

echo.
echo Starting application with Java 11 compatibility...
echo Web interface will be available at: http://localhost:8080
echo.

REM Run the application with Java 11 compatibility
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dorg.slf4j.simpleLogger.defaultLogLevel=INFO -Dorg.slf4j.simpleLogger.log.com.bitunix.scalper=DEBUG --add-modules java.xml.bind --add-modules java.activation"

pause
