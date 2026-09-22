@echo off
chcp 65001 > nul
title UNO Game Multiplayer (PTIT LTM) - Launcher

echo ====================================================================
echo               UNO MULTIPLAYER ONLINE - PTIT LTM
echo ====================================================================

if not exist bin mkdir bin

echo [1/2] Đang biên dịch mã nguồn Java (javac 1.8)...
javac -encoding UTF-8 -d bin -sourcepath src src\com\ptit\uno\model\*.java src\com\ptit\uno\protocol\*.java src\com\ptit\uno\server\*.java src\com\ptit\uno\server\dao\*.java src\com\ptit\uno\server\view\*.java src\com\ptit\uno\server\control\*.java src\com\ptit\uno\client\*.java src\com\ptit\uno\client\view\*.java src\com\ptit\uno\client\control\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [LỖI] Quá trình biên dịch thất bại! Vui lòng kiểm tra lại.
    pause
    exit /b %ERRORLEVEL%
)

if not exist bin\resource mkdir bin\resource
xcopy /s /y /i src\resource bin\resource > nul

echo [2/2] Biên dịch thành công 100%! (Đã nạp bộ ảnh UNO Cards)
echo.
echo ====================================================================
echo MENU KHỞI CHẠY:
echo [1] Chạy SERVER (ServerRun)
echo [2] Chạy CLIENT (ClientRun)
echo [3] Chạy 1 Server + 2 Client cùng lúc (Demo nhanh)
echo [4] Thoát
echo ====================================================================
set /p choice="Nhập lựa chọn của bạn (1-4): "

if "%choice%"=="1" (
    echo Đang khởi chạy Server trên port 8888...
    java -cp bin com.ptit.uno.server.ServerRun
) else if "%choice%"=="2" (
    echo Đang khởi chạy Client...
    java -cp bin com.ptit.uno.client.ClientRun
) else if "%choice%"=="3" (
    echo Đang khởi chạy Server và 2 Client giả lập...
    start "UNO Server" java -cp bin com.ptit.uno.server.ServerRun
    timeout /t 2 /nobreak > nul
    start "UNO Player 1" java -cp bin com.ptit.uno.client.ClientRun
    timeout /t 1 /nobreak > nul
    start "UNO Player 2" java -cp bin com.ptit.uno.client.ClientRun
) else (
    exit /b 0
)
