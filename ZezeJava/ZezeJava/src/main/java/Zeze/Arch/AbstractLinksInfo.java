// auto-generated @formatter:off
package Zeze.Arch;

public abstract class AbstractLinksInfo implements Zeze.IModule {
    public static final int ModuleId = 11032;
    public static final String ModuleName = "LinksInfo";
    public static final String ModuleFullName = "Zeze.Arch.LinksInfo";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

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
        var _reflect = new Zeze.Util.Reflect(getClass());
        httpServer.addHandler("/Zeze/Builtin/LinksInfo/LinksTextMultiLine", 8192,
                _reflect.getTransactionLevel("OnServletLinksTextMultiLine", Zeze.Transaction.TransactionLevel.None),
                _reflect.getDispatchMode("OnServletLinksTextMultiLine", Zeze.Transaction.DispatchMode.Normal),
                this::OnServletLinksTextMultiLine);
        httpServer.addHandler("/Zeze/Builtin/LinksInfo/LinksTextSingleLine", 8192,
                _reflect.getTransactionLevel("OnServletLinksTextSingleLine", Zeze.Transaction.TransactionLevel.None),
                _reflect.getDispatchMode("OnServletLinksTextSingleLine", Zeze.Transaction.DispatchMode.Normal),
                this::OnServletLinksTextSingleLine);
    }

    protected abstract void OnServletLinksTextMultiLine(Zeze.Netty.HttpExchange x) throws Exception;
    protected abstract void OnServletLinksTextSingleLine(Zeze.Netty.HttpExchange x) throws Exception;
}
