@echo off
chcp 65001 > nul
echo ===================================================================
echo   BREW ^& BEAN CAFE - MAY CHU ^& QUAY PHA CHE TRUNG TAM (SERVERMAIN)
echo   Chuc nang: TCP Socket Engine ^& Man Hinh KDS Pha Che
echo ===================================================================
java "-Dfile.encoding=UTF-8" -XX:-PrintWarnings -cp "build/classes;lib/*" cafe.ServerMain
pause