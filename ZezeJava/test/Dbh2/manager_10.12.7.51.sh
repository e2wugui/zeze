#!/bin/bash

classes=../../ZezeJava/build/classes/java/main
java -Dlogname=Manage_ -cp %classes%;../../ZezeJava/lib/*;. Zeze.Dbh2.Dbh2Manager manager_10.12.7.51 zeze_10.12.7.51.xml &
