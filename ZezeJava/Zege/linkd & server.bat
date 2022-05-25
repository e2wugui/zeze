
rem start ..\gradlew.bat start_linkd

set classes=../ZezeJava/build/classes/java/main;linkd/build/classes/java/main
start "linkd" java -cp %classes%;lib/* Zege.Program

rem start ..\gradlew.bat start_server

set classes=../ZezeJava/build/classes/java/main;server/build/classes/java/main
start "server" java -cp %classes%;lib/* Zege.Program
