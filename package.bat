@echo off
setlocal enabledelayedexpansion

REM ============================================================
REM  missive — windows packaging script
REM  builds a standalone Missive.exe with bundled JRE (no install)
REM ============================================================

set JFX_VER=21.0.5
set JFX_BASE=%USERPROFILE%\.m2\repository\org\openjfx
set JDK=C:\Program Files\Java\jdk-26
set OUT=dist

REM sanity
if not exist "%JDK%\bin\jpackage.exe" (
    echo [ERROR] jpackage not found at "%JDK%\bin\jpackage.exe"
    echo edit JDK= at the top of this script
    exit /b 1
)

echo.
echo === [1/5] building project ===
call mvnw.cmd -q -DskipTests package
if errorlevel 1 ( echo build failed & exit /b 1 )

echo.
echo === [2/5] copying non-javafx deps to target\applib ===
rmdir /s /q target\applib 2>nul
call mvnw.cmd -q dependency:copy-dependencies -DoutputDirectory=target/applib -DexcludeGroupIds=org.openjfx -DincludeScope=runtime
copy /y target\missive-1.0.0.jar target\applib\ >nul

echo.
echo === [3/5] resolving javafx module path ===
set MP=
for %%M in (base graphics controls fxml) do (
    set "MP=!MP!;%JFX_BASE%\javafx-%%M\%JFX_VER%\javafx-%%M-%JFX_VER%.jar"
    set "MP=!MP!;%JFX_BASE%\javafx-%%M\%JFX_VER%\javafx-%%M-%JFX_VER%-win.jar"
)
REM strip leading semicolon
set "MP=!MP:~1!"

echo.
echo === [4/5] building custom runtime via jlink ===
rmdir /s /q target\runtime 2>nul
"%JDK%\bin\jlink.exe" ^
  --module-path "%MP%" ^
  --add-modules java.base,java.sql,java.naming,java.desktop,java.logging,jdk.unsupported,javafx.controls,javafx.fxml ^
  --strip-debug --no-header-files --no-man-pages --compress=zip-6 ^
  --output target\runtime
if errorlevel 1 ( echo jlink failed & exit /b 1 )

echo.
echo === [5/5] running jpackage ===
rmdir /s /q %OUT% 2>nul
"%JDK%\bin\jpackage.exe" ^
  --type app-image ^
  --name Missive ^
  --app-version 1.0.0 ^
  --vendor "missive" ^
  --description "secure desktop messenger" ^
  --input target\applib ^
  --main-jar missive-1.0.0.jar ^
  --main-class missive.Main ^
  --runtime-image target\runtime ^
  --java-options "--enable-native-access=ALL-UNNAMED" ^
  --dest %OUT%
if errorlevel 1 ( echo jpackage failed & exit /b 1 )

echo.
echo ============================================================
echo  done.
echo  run:    %OUT%\Missive\Missive.exe
echo  size:   ~80 MB (full app + bundled JRE + JavaFX)
echo  ship:   zip the entire %OUT%\Missive folder
echo ============================================================
