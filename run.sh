#!/bin/sh
# macOS / Linux 一鍵編譯並執行
javac -encoding UTF-8 -d out src/maze/*.java && java -cp out maze.Main
