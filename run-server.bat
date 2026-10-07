@echo off
chcp 65001 > nul
echo ===================================================
echo   KHOI DONG QUAY PHA CHE (SERVER KDS - JAVAFX)
echo ===================================================
java "-Dfile.encoding=UTF-8" -XX:-PrintWarnings -cp "build/classes;lib/*" cafe.ServerMain
pause
