
export classes=linkd/build/classes/java/main:../ZezeJava/build/classes/java/main
java -Dlogname=linkd -cp $classes:../ZezeJava/lib/*:. Zege.Program -zezeconf linkd.xml &
java -Dlogname=linkd -cp $classes:../ZezeJava/lib/*:. Zege.Program -zezeconf linkd1.xml &

export classes=server/build/classes/java/main:../ZezeJava/build/classes/java/main
java -Dlogname=server -cp $classes:../ZezeJava/lib/*:. Zege.Program -zezeconf server.xml &
java -Dlogname=server -cp $classes:../ZezeJava/lib/*:. Zege.Program -zezeconf server1.xml &
