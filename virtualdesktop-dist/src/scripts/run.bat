@echo off
REM VirtualDesktop Launch Script for Windows
REM
REM This script launches VirtualDesktop with the appropriate classpath
REM and JVM options.

setlocal

REM Determine the installation directory
set "SCRIPT_DIR=%~dp0"
set "APP_HOME=%SCRIPT_DIR%.."

REM Build classpath from lib directory
set "CLASSPATH="
for %%f in ("%APP_HOME%\lib\*.jar") do (
    if defined CLASSPATH (
        set "CLASSPATH=!CLASSPATH!;%%f"
    ) else (
        set "CLASSPATH=%%f"
    )
)

REM Enable delayed expansion for classpath building
setlocal enabledelayedexpansion

set "CLASSPATH="
for %%f in ("%APP_HOME%\lib\*.jar") do (
    if defined CLASSPATH (
        set "CLASSPATH=!CLASSPATH!;%%f"
    ) else (
        set "CLASSPATH=%%f"
    )
)

REM JVM options
set "JAVA_OPTS=-Xmx512m"

REM For Java 9+, add required module access
REM Uncomment the following line if running on Java 9+
REM set "JAVA_OPTS=%JAVA_OPTS% --add-opens=java.base/jdk.internal.loader=ALL-UNNAMED"

REM Launch the application
java %JAVA_OPTS% -cp "%CLASSPATH%" org.jwellman.virtualdesktop.App %*

endlocal
