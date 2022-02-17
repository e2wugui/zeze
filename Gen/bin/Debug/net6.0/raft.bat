@echo off
setlocal
pushd %~dp0

del *.log

start "raft.xml"   gen -c RaftTest -Config raft.xml

start "raft3.xml"  gen -c RaftTest -Config raft3.xml
start "raft6.xml"  gen -c RaftTest -Config raft6.xml
start "raft9.xml"  gen -c RaftTest -Config raft9.xml

start "raft12.xml" gen -c RaftTest -Config raft12.xml
start "raft15.xml" gen -c RaftTest -Config raft15.xml
start "raft18.xml" gen -c RaftTest -Config raft18.xml

start "raft21.xml" gen -c RaftTest -Config raft21.xml
start "raft24.xml" gen -c RaftTest -Config raft24.xml
start "raft27.xml" gen -c RaftTest -Config raft27.xml
