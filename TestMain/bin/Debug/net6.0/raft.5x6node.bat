@echo off
setlocal
pushd %~dp0

del raft.debug.5x6node.6500_6504.xml.log
start "raft.debug.5x6node.6500_6504.xml"   TestMain -c RaftTest -Config raft.debug.5x6node.6500_6504.xml

del raft.debug.5x6node.6505_6509.xml.log
start "raft.debug.5x6node.6505_6509.xml"  TestMain -c RaftTest -Config raft.debug.5x6node.6505_6509.xml

del raft.debug.5x6node.6510_6514.xml.log
start "raft.debug.5x6node.6510_6514.xml"  TestMain -c RaftTest -Config raft.debug.5x6node.6510_6514.xml

del raft.debug.5x6node.6515_6519.xml.log
start "raft.debug.5x6node.6515_6519.xml"  TestMain -c RaftTest -Config raft.debug.5x6node.6515_6519.xml

del raft.debug.5x6node.6520_6524.xml.log
start "raft.debug.5x6node.6520_6524.xml"  TestMain -c RaftTest -Config raft.debug.5x6node.6520_6524.xml

