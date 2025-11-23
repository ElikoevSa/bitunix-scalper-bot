@echo off
echo Starting Bitunix Scalper Bot (Optimized Version)...
echo.

REM Set Maven environment variables for this session
set MAVEN_HOME=C:\apache-maven\apache-maven-3.9.5
set PATH=%PATH%;C:\apache-maven\apache-maven-3.9.5\bin

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 11 or higher
    pause
    exit /b 1
)

REM Check if Maven is available
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven is not available
    echo Please check MAVEN_HOME and PATH settings
    pause
    exit /b 1
)

echo Java and Maven are available
echo.

REM Create logs directory
if not exist "logs" mkdir logs

REM Clean and compile the project
echo Compiling project...
mvn clean compile
if %errorlevel% neq 0 (
    echo ERROR: Compilation failed
    pause
    exit /b 1
)

echo.
echo Starting application...
echo.
echo 🌐 Web interfaces available at:
echo    Main Dashboard: http://localhost:8080
echo    Test Page:      http://localhost:8080/test.html
echo    API Status:     http://localhost:8080/api/test/status
echo    Rate Limiter:   http://localhost:8080/api/rate-limiter/status
echo    H2 Console:     http://localhost:8080/h2-console
echo.

REM Run the application
mvn spring-boot:run

pause
