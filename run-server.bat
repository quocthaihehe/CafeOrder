@echo off
chcp 65001 > nul
echo ===================================================================
echo   BREW & BEAN CAFE - COFFEPOS QUẦY TRUNG TÂM (SERVERMAIN)
echo   Chức năng: Quản Trị Admin, Dashboard Doanh Thu, Barista KDS
echo ===================================================================
java "-Dfile.encoding=UTF-8" -XX:-PrintWarnings -cp "build/classes;lib/*" cafe.ServerMain
pause
