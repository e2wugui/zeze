@echo off
setlocal
pushd %~dp0

del raft.xml.log
del raft3.xml.log
del raft6.xml.log
del raft9.xml.log
del raft12.xml.log

start "raft.xml"   gen -c RaftTest -Config raft.xml
start "raft3.xml"  gen -c RaftTest -Config raft3.xml
start "raft6.xml"  gen -c RaftTest -Config raft6.xml
start "raft9.xml"  gen -c RaftTest -Config raft9.xml
start "raft12.xml" gen -c RaftTest -Config raft12.xml

