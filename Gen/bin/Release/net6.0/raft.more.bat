@echo off
setlocal
pushd %~dp0

del raft9.xml.log
start "R raft9.xml"  gen -c RaftTest -Config raft9.xml

del raft12.xml.log
start "R raft12.xml" gen -c RaftTest -Config raft12.xml

del raft15.xml.log
start "R raft15.xml" gen -c RaftTest -Config raft15.xml

del raft18.xml.log
start "R raft18.xml" gen -c RaftTest -Config raft18.xml

del raft21.xml.log
start "R raft21.xml" gen -c RaftTest -Config raft21.xml

del raft24.xml.log
start "R raft24.xml" gen -c RaftTest -Config raft24.xml

del raft27.xml.log
start "R raft27.xml" gen -c RaftTest -Config raft27.xml
