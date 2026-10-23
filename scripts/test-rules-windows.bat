@echo off
setlocal
cd /d "%~dp0.."
set "OUT=%TEMP%\chamado-regras-%RANDOM%-%RANDOM%"
mkdir "%OUT%"
javac --release 17 -encoding UTF-8 -d "%OUT%" backend\src\main\java\br\com\chamadorapido\TicketRules.java backend\src\main\java\br\com\chamadorapido\TriageEngine.java backend\src\test\java\br\com\chamadorapido\PureRulesCheck.java
if errorlevel 1 goto failed
java -cp "%OUT%" br.com.chamadorapido.PureRulesCheck
if errorlevel 1 goto failed
rmdir /s /q "%OUT%"
pause
exit /b 0
:failed
rmdir /s /q "%OUT%"
echo Falha nos testes de regras. Confira o erro acima.
pause
exit /b 1
