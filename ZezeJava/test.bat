@echo off

cd /d %~dp0

call gradlew.bat --continue --rerun-tasks :ZezeJavaTest:test

call gradlew.bat --continue --rerun-tasks :ZezeJavaTest:extraTest

call gradlew.bat --continue --rerun-tasks :ZezeJavaTest:integrationTest

call gradlew.bat --continue --rerun-tasks :ZezeJavaTest:bench

pause
