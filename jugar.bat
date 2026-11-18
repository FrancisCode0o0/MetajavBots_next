@echo off
chcp 65001 >nul
REM MetajavBots: compila lo que haga falta y abre el juego.
REM "jugar.bat prueba": las pruebas sin ventana. "jugar.bat capturas": fotos de todas las pantallas en build\capturas. "jugar.bat estilo": Checkstyle.
setlocal
cd /d "%~dp0"
if /i "%~1"=="prueba" (
    call "%~dp0gradlew.bat" -q pruebas:prueba
    exit /b %errorlevel%
)
if /i "%~1"=="estilo" (
    call "%~dp0gradlew.bat" -q estilo
    exit /b %errorlevel%
)
if /i "%~1"=="capturas" (
    call "%~dp0gradlew.bat" -q lwjgl3:capturas
    exit /b %errorlevel%
)
call "%~dp0gradlew.bat" -q lwjgl3:run
