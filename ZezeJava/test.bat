@echo off

cd /d %~dp0

call gradlew.bat --rerun-tasks :ZezeJavaTest:test

call gradlew.bat --rerun-tasks :ZezeJavaTest:integrationTest

call gradlew.bat --rerun-tasks :ZezeJavaTest:bench

pause
