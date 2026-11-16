@echo off
rem iniciar.bat: sobe o backend (porta 8080) e o front (porta 5500) em duas janelas. Feche as janelas para parar.
rem Rode o adaptar.exe uma vez antes.
setlocal
chcp 65001 >nul
cd /d "%~dp0"

if not exist ".ferramentas\banco.conf" (
    echo ERRO: rode o adaptar.exe antes ^(ele prepara o Java, as bibliotecas e o banco^).
    pause
    exit /b 1
)

rem le PG_HOST, PG_PORTA, PG_BANCO, PG_USER e PG_SENHA
for /f "usebackq tokens=1,* delims==" %%a in (".ferramentas\banco.conf") do set "%%a=%%b"

rem usa o JDK baixado pelo adaptar, se existir; senao o Java do sistema
set "JWEB=jwebserver"
if exist ".ferramentas\jdk\bin\java.exe" (
    set "JAVA_HOME=%CD%\.ferramentas\jdk"
    set "JWEB=%CD%\.ferramentas\jdk\bin\jwebserver.exe"
)

set "SPRING_DATASOURCE_URL=jdbc:postgresql://%PG_HOST%:%PG_PORTA%/%PG_BANCO%"
set "SPRING_DATASOURCE_USERNAME=%PG_USER%"
set "SPRING_DATASOURCE_PASSWORD=%PG_SENHA%"

netstat -ano | findstr /R /C:":8080 .*LISTENING" >nul && (
    echo ERRO: a porta 8080 ja esta em uso ^(outro backend rodando, talvez no IntelliJ^). Feche-o e tente de novo.
    pause
    exit /b 1
)
netstat -ano | findstr /R /C:":5500 .*LISTENING" >nul && (
    echo ERRO: a porta 5500 ja esta em uso. Feche o que a usa e tente de novo.
    pause
    exit /b 1
)

start "Mercado FIPP - backend" /d "%CD%\backend" cmd /k mvnw.cmd -B -ntp spring-boot:run
start "Mercado FIPP - front" cmd /k ""%JWEB%" -d "%CD%\frontend" -p 5500"

echo.
echo Subindo. Quando a janela do backend mostrar "Started BackendApplication", abra:
echo     http://localhost:5500/
echo Para parar, feche as duas janelas.
echo.
pause
