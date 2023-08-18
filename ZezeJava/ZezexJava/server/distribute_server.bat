
@echo off
setlocal
pushd %~dp0

set classes=../../ZezeJava/build/classes/java/main
set hotrun=hotrun/*;hotrun/modules/*;hotrun/interfaces/*

echo "启动发布包管理服务器，用于c#.Gen查询发布包的Bean的结构"

java -cp %classes%;%hotrun%;../../ZezeJavaTest/lib/jgrapht-core-1.5.2.jar;../../ZezeJava/lib/* Zeze.Hot.DistributeServer -solution Game

