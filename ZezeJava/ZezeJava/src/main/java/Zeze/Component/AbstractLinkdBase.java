// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractLinkdBase implements Zeze.IModule {
    public static final int ModuleId = 11011;
    @Override public String getFullName() { return "Zeze.Component.LinkdBase"; }
    @Override public String getName() { return "LinkdBase"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    public void RegisterHttpServlet(Zeze.Netty.HttpServer httpServer) {
    }

}
