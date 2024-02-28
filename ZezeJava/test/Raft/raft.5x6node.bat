@echo off
setlocal
pushd %~dp0

start /low "raft5x6.6500_6504.xml" java -Dlogname=raft5x6_6500_6504 -cp .;..\..\ZezeJava\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.4.2-SNAPSHOT.jar Zeze.Raft.Test -c RaftTest -Config raft.debug.5x6node.6500_6504.xml
start /low "raft5x6.6505_6509.xml" java -Dlogname=raft5x6_6505_6509 -cp .;..\..\ZezeJava\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.4.2-SNAPSHOT.jar Zeze.Raft.Test -c RaftTest -Config raft.debug.5x6node.6505_6509.xml
start /low "raft5x6.6510_6514.xml" java -Dlogname=raft5x6_6510_6514 -cp .;..\..\ZezeJava\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.4.2-SNAPSHOT.jar Zeze.Raft.Test -c RaftTest -Config raft.debug.5x6node.6510_6514.xml
start /low "raft5x6.6515_6519.xml" java -Dlogname=raft5x6_6515_6519 -cp .;..\..\ZezeJava\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.4.2-SNAPSHOT.jar Zeze.Raft.Test -c RaftTest -Config raft.debug.5x6node.6515_6519.xml
start /low "raft5x6.6520_6524.xml" java -Dlogname=raft5x6_6520_6524 -cp .;..\..\ZezeJava\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.4.2-SNAPSHOT.jar Zeze.Raft.Test -c RaftTest -Config raft.debug.5x6node.6520_6524.xml
