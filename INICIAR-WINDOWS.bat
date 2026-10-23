@echo off
setlocal
cd /d "%~dp0"
echo.
echo CHAMADO RAPIDO - Angular + Java 17 + PostgreSQL
echo Tecnologia que simplifica, suporte que resolve.
echo.
where docker >nul 2>nul
if errorlevel 1 (
 echo ERRO: instale e abra o Docker Desktop. Consulte COMECE-AQUI.html.
 pause
 exit /b 1
)
docker info >nul 2>nul
if errorlevel 1 (
 echo ERRO: Docker nao esta em execucao. Abra o Docker Desktop.
 pause
 exit /b 1
)
docker compose version >nul 2>nul
if errorlevel 1 (
 echo ERRO: este projeto requer o Docker Compose v2.
 pause
 exit /b 1
)
if not exist .env (
 copy /y .env.example .env >nul
 echo Criado .env de DEMONSTRACAO LOCAL. Nao use dados reais.
)
echo A primeira construcao baixa dependencias e exige internet.
docker compose up --build -d --wait --wait-timeout 180
if errorlevel 1 (
 echo Nao foi possivel iniciar. Veja a mensagem acima.
 echo Consulte: docker compose logs --tail=100 app
 pause
 exit /b 1
)
for /f "tokens=1,* delims==" %%A in ('findstr /b "APP_PORT=" .env') do set "APP_PORT=%%B"
if not defined APP_PORT set "APP_PORT=8080"
echo Aplicacao iniciada em http://localhost:%APP_PORT%
start "Chamado Rapido" "http://localhost:%APP_PORT%"
echo Para encerrar sem perder dados: docker compose down
pause
