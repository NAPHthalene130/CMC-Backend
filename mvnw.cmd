@echo off
set "DIR=%~dp0"
set "WRAPPER_JAR=%DIR%.mvn\wrapper\maven-wrapper.jar"

if not exist "%WRAPPER_JAR%" (
    echo Downloading Maven Wrapper...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar', '%WRAPPER_JAR%')"
    if not exist "%WRAPPER_JAR%" (
        echo ERROR: Failed to download Maven Wrapper JAR
        exit /b 1
    )
)

set "JDK_BIN=%JAVA_HOME%\bin\java.exe"
if not exist "%JDK_BIN%" set "JDK_BIN=java.exe"

"%JDK_BIN%" -classpath "%WRAPPER_JAR%" -Dmaven.multiModuleProjectDirectory="%DIR%" org.apache.maven.wrapper.MavenWrapperMain %*
exit /b %ERRORLEVEL%
