@echo off
chcp 65001 > nul
echo ===================================================================
echo   BREW & BEAN CAFE - THIẾT BỊ ĐẦU CUỐI SẢNH (CLIENTMAIN)
echo   Chức năng: Kiosk Gọi Món Tại Bàn (Khách) / iPad Phục Vụ (Nhân Viên)
echo ===================================================================
java "-Dfile.encoding=UTF-8" -XX:-PrintWarnings -cp "build/classes;lib/*" cafe.ClientMain
pause
