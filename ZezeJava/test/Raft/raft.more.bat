@echo off
setlocal
pushd %~dp0

start /low "raft9.xml"   java -Dlogname=raft9  -cp .;..\..\ZezeJava\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.4.2-SNAPSHOT.jar Zeze.Raft.Test -c RaftTest -Config raft9.xml
start /low "raft12.xml"  java -Dlogname=raft12 -cp .;..\..\ZezeJava\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.4.2-SNAPSHOT.jar Zeze.Raft.Test -c RaftTest -Config raft12.xml
start /low "raft15.xml"  java -Dlogname=raft15 -cp .;..\..\ZezeJava\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.4.2-SNAPSHOT.jar Zeze.Raft.Test -c RaftTest -Config raft15.xml
start /low "raft18.xml"  java -Dlogname=raft18 -cp .;..\..\ZezeJava\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.4.2-SNAPSHOT.jar Zeze.Raft.Test -c RaftTest -Config raft18.xml
start /low "raft21.xml"  java -Dlogname=raft21 -cp .;..\..\ZezeJava\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.4.2-SNAPSHOT.jar Zeze.Raft.Test -c RaftTest -Config raft21.xml
start /low "raft24.xml"  java -Dlogname=raft24 -cp .;..\..\ZezeJava\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.4.2-SNAPSHOT.jar Zeze.Raft.Test -c RaftTest -Config raft24.xml
start /low "raft27.xml"  java -Dlogname=raft27 -cp .;..\..\ZezeJava\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.4.2-SNAPSHOT.jar Zeze.Raft.Test -c RaftTest -Config raft27.xml
