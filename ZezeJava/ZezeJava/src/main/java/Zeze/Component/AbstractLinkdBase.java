// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractLinkdBase implements Zeze.IModule {
    public static final int ModuleId = 11011;
    public static final String ModuleName = "LinkdBase";
    public static final String ModuleFullName = "Zeze.Component.LinkdBase";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
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
