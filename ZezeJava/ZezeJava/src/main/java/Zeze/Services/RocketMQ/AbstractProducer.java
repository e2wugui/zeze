// auto-generated @formatter:off
package Zeze.Services.RocketMQ;

public abstract class AbstractProducer implements Zeze.IModule {
    public static final int ModuleId = 11024;
    public static final String ModuleName = "Producer";
    public static final String ModuleFullName = "Zeze.Services.RocketMQ.Producer";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    protected final Zeze.Builtin.RocketMQ.Producer.tSent _tSent = new Zeze.Builtin.RocketMQ.Producer.tSent();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tSent.getName()).getDatabaseName(), _tSent);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tSent.getName()).getDatabaseName(), _tSent);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    public void RegisterHttpServlet(Zeze.Netty.HttpServer httpServer) {
    }

}
