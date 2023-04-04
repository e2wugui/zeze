#!/bin/bash

classes=../../ZezeJava/build/classes/java/main
java -Dlogname=Master -cp $classes:../../ZezeJava/lib/*:. Zeze.Dbh2.Master.Main zeze_10.12.7.155.xml &
java -Dlogname=Manage_ -cp $classes:../../ZezeJava/lib/*:. Zeze.Dbh2.Dbh2Manager manager_10.12.7.155 zeze_10.12.7.155.xml &
