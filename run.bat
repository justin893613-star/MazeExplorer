@echo off
rem Windows 一鍵編譯並執行（先切換為 UTF-8 碼頁，中文與方塊字才不會變亂碼）
chcp 65001 > nul
javac -encoding UTF-8 -d out src\maze\*.java && java -Dstdout.encoding=UTF-8 -cp out maze.Main
pause
