@echo off
chcp 65001 > nul
echo ===================================================================
echo   BREW ^& BEAN CAFE - UNG DUNG TRAM (CLIENTMAIN - 3 ROLE)
echo   Chuc nang: Kiosk Khach Hang / iPad Nhan Vien / Quan Tri Admin
echo ===================================================================
java "-Dfile.encoding=UTF-8" -XX:-PrintWarnings -cp "build/classes;lib/*" cafe.ClientMain
pause