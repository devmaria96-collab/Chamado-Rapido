@echo off
setlocal
cd /d "%~dp0.."
set "ROOT=%CD%"
where java >nul 2>nul
if errorlevel 1 goto requirements
where mvn >nul 2>nul
if errorlevel 1 goto requirements
where npm >nul 2>nul
if errorlevel 1 goto requirements
cd /d "%ROOT%\frontend"
if exist package-lock.json (call npm ci) else (call npm install)
if errorlevel 1 goto failed
cd /d "%ROOT%"
start "Chamado Rapido - API Java 17" /D "%ROOT%\backend" cmd /k "mvn spring-boot:run"
start "Chamado Rapido - Angular" /D "%ROOT%\frontend" cmd /k "npm start"
echo Acesse http://localhost:4200 quando os dois terminais estiverem prontos.
echo Ambiente local com H2 persistente e dados ficticios.
pause
exit /b 0
:requirements
echo Requisitos: JDK 17, Maven 3.9 e Node.js 22.12 ou superior da linha 22.
echo Confira java -version, mvn -version e node -v.
pause
exit /b 1
:failed
echo A instalacao das dependencias falhou. Verifique a conexao e a mensagem acima.
pause
exit /b 1
