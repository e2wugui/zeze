
@echo off
setlocal
pushd %~dp0

set classes=../../ZezeJava/build/classes/java/main
set hotrun=hotrun/*;hotrun/modules/*;hotrun/interfaces/*

java -cp %classes%;%hotrun%;../../ZezeJavaTest/lib/jgrapht-core-1.5.2.jar;../../ZezeJava/lib/* Zeze.Hot.DistributeServer -solution Game

