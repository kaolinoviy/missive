@REM Maven Wrapper batch file
@echo off

set MAVEN_VERSION=3.9.9
set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\apache-maven-%MAVEN_VERSION%
set MAVEN_ZIP=%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip
set MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Downloading Apache Maven %MAVEN_VERSION%...
    powershell -Command "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MAVEN_ZIP%'" || goto error
    powershell -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%USERPROFILE%\.m2\wrapper\' -Force" || goto error
    del "%MAVEN_ZIP%"
    echo Maven %MAVEN_VERSION% downloaded.
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
goto end

:error
echo Failed to download Maven. Install it manually from https://maven.apache.org/download.cgi
exit /b 1

:end
