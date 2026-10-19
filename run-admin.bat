@echo off
chcp 65001 > nul
echo ===================================================
echo   KHOI DONG TRANG QUAN TRI (ADMIN PORTAL - JAVAFX)
echo ===================================================
java "-Dfile.encoding=UTF-8" -XX:-PrintWarnings -cp "build/classes;lib/*" cafe.AdminMain
pause
