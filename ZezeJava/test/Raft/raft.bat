@echo off
setlocal
pushd %~dp0

start /low "raft.xml"  java -Dlogname=raft  -cp .;..\..\ZezeJava\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.4.2-SNAPSHOT.jar Zeze.Raft.Test -c RaftTest -Config raft.xml
start /low "raft3.xml" java -Dlogname=raft3 -cp .;..\..\ZezeJava\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.4.2-SNAPSHOT.jar Zeze.Raft.Test -c RaftTest -Config raft3.xml
start /low "raft6.xml" java -Dlogname=raft6 -cp .;..\..\ZezeJava\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.4.2-SNAPSHOT.jar Zeze.Raft.Test -c RaftTest -Config raft6.xml
