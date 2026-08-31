@ECHO OFF
SETLOCAL
SET DIST_VERSION=8.14.3
SET DIST_DIR=%USERPROFILE%\.gradle\wrapper\dists\gradle-%DIST_VERSION%-bin
SET GRADLE_HOME=%DIST_DIR%\gradle-%DIST_VERSION%
SET ZIP=%DIST_DIR%\gradle-%DIST_VERSION%-bin.zip
IF NOT EXIST "%GRADLE_HOME%\bin\gradle.bat" (
  IF NOT EXIST "%DIST_DIR%" MKDIR "%DIST_DIR%"
  IF NOT EXIST "%ZIP%" powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri 'https://services.gradle.org/distributions/gradle-%DIST_VERSION%-bin.zip' -OutFile '%ZIP%'"
  IF EXIST "%GRADLE_HOME%" RMDIR /S /Q "%GRADLE_HOME%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP%' '%DIST_DIR%'"
)
CALL "%GRADLE_HOME%\bin\gradle.bat" %*
ENDLOCAL
