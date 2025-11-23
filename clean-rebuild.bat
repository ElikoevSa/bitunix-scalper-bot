@echo off
echo Cleaning Maven cache and rebuilding project...
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

REM Clean Maven cache
echo Cleaning Maven cache...
mvn dependency:purge-local-repository -DmanualInclude="org.glassfish.jaxb:jaxb-core"
if %errorlevel% neq 0 (
    echo Warning: Failed to purge specific dependency, trying full clean...
    mvn dependency:purge-local-repository
)

echo.
echo Cleaning project...
mvn clean
if %errorlevel% neq 0 (
    echo ERROR: Clean failed
    pause
    exit /b 1
)

echo.
echo Updating dependencies...
mvn dependency:resolve -U
if %errorlevel% neq 0 (
    echo Warning: Dependency resolve failed, continuing with compile...
)

echo.
echo Compiling project...
mvn compile -U
if %errorlevel% neq 0 (
    echo ERROR: Compilation failed
    pause
    exit /b 1
)

echo.
echo Build successful! You can now run the application.
echo.

pause
