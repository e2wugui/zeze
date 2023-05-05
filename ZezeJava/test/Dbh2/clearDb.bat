@echo off
setlocal
pushd %~dp0

rd /s/q CommitRocks 2>nul
rd /s/q log 2>nul
rd /s/q manager0 2>nul
rd /s/q manager1 2>nul
rd /s/q manager2 2>nul
rd /s/q master 2>nul
del "dbh2.raft,0.zeze.pal" 2>nul

pause
