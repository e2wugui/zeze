
#start ServiceManager & Global
java -Dlogname=ServiceManagerServer -cp zeze/main:lib/*:. Zeze.Services.ServiceManagerServer &
java -Dlogname=GlobalCacheManagerAsyncServer -cp zeze/main:lib/*:. Zeze.Services.GlobalCacheManagerAsyncServer &

#start linkd
java -Dlogname=linkd -cp linkd/main:zeze/main:lib/*:. Zege.Program -zezeconf linkd.xml &
java -Dlogname=linkd -cp linkd/main:zeze/main:lib/*:. Zege.Program -zezeconf linkd1.xml &

#start server
java -Dlogname=server -cp server/main:zeze/main:lib/*:. Zege.Program -perf -zezeconf server.xml &
java -Dlogname=server -cp server/main:zeze/main:lib/*:. Zege.Program -perf -zezeconf server1.xml &
