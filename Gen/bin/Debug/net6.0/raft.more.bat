@echo off
setlocal
pushd %~dp0

del raft15.xml.log
del raft18.xml.log
del raft21.xml.log
del raft24.xml.log
del raft27.xml.log

start "raft15.xml" gen -c RaftTest -Config raft15.xml
start "raft18.xml" gen -c RaftTest -Config raft18.xml
start "raft21.xml" gen -c RaftTest -Config raft21.xml
start "raft24.xml" gen -c RaftTest -Config raft24.xml
start "raft27.xml" gen -c RaftTest -Config raft27.xml
