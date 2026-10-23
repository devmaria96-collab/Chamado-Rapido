@echo off
setlocal
cd /d "%~dp0.."
set "ROOT=%CD%"
cd frontend
if exist package-lock.json (call npm ci) else (call npm install)
if errorlevel 1 goto failed
call npm run build
if errorlevel 1 goto failed
cd /d "%ROOT%"
if exist backend\src\main\resources\static rmdir /s /q backend\src\main\resources\static
mkdir backend\src\main\resources\static
xcopy frontend\dist\chamado-rapido\browser backend\src\main\resources\static /E /I /Y >nul
if errorlevel 1 goto failed
cd backend
call mvn clean verify
if errorlevel 1 goto failed
cd /d "%ROOT%"
if not exist executavel mkdir executavel
copy /y backend\target\chamado-rapido.jar executavel\chamado-rapido.jar >nul
echo Compilacao e testes Maven concluidos. Gerado executavel\chamado-rapido.jar.
echo Para demonstracao local: java -jar executavel\chamado-rapido.jar
pause
exit /b 0
:failed
echo Houve uma falha. O artefato nao foi validado. Leia o erro acima.
pause
exit /b 1
