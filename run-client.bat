@echo off
chcp 65001 > nul
echo ===================================================
echo   KHOI DONG MAN HINH DAT MON TAI BAN (CLIENT KIOSK)
echo ===================================================
java "-Dfile.encoding=UTF-8" -XX:-PrintWarnings -cp "build/classes;lib/*" cafe.ClientMain
pause
