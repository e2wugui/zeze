@echo off
setlocal
pushd %~dp0

del raft.release.5x6node.7500_7504.xml.log
start "raft.release.5x6node.7500_7504.xml"   gen -c RaftTest -Config raft.release.5x6node.7500_7504.xml

del raft.release.5x6node.7505_7509.xml.log
start "raft.release.5x6node.7505_7509.xml"  gen -c RaftTest -Config raft.release.5x6node.7505_7509.xml

del raft.release.5x6node.7510_7514.xml.log
start "raft.release.5x6node.7510_7514.xml"  gen -c RaftTest -Config raft.release.5x6node.7510_7514.xml

del raft.release.5x6node.7515_7519.xml.log
start "raft.release.5x6node.7515_7519.xml"  gen -c RaftTest -Config raft.release.5x6node.7515_7519.xml

del raft.release.5x6node.7520_7524.xml.log
start "raft.release.5x6node.7520_7524.xml"  gen -c RaftTest -Config raft.release.5x6node.7520_7524.xml

