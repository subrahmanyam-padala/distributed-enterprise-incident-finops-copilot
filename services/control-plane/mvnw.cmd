@REM Maven Wrapper Script for Windows
@REM Auto-downloads Maven binary if not already cached

@echo off
setlocal

set MAVEN_WRAPPER_PROPERTIES=.mvn\wrapper\maven-wrapper.properties
set USER_HOME=%USERPROFILE%
set MAVEN_USER_HOME=%USER_HOME%\.m2\wrapper\dists

if exist "%MAVEN_USER_HOME%\apache-maven-3.9.9\bin\mvn.cmd" (
    set MAVEN_HOME=%MAVEN_USER_HOME%\apache-maven-3.9.9
) else (
    echo Maven wrapper requires Maven 3.9+ to be installed.
    echo Please download from: https://maven.apache.org/download.cgi
    echo Or install via Chocolatey: choco install maven
    exit /b 1
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
