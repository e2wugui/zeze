// auto-generated @formatter:off
package Zeze.History;

public abstract class AbstractHistoryModule implements Zeze.IModule {
    public static final int ModuleId = 11031;
    public static final String ModuleName = "HistoryModule";
    public static final String ModuleFullName = "Zeze.History.HistoryModule";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    protected final Zeze.Builtin.HistoryModule.ZezeHistoryTable_m_a_g_i_c _ZezeHistoryTable_m_a_g_i_c = new Zeze.Builtin.HistoryModule.ZezeHistoryTable_m_a_g_i_c();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_ZezeHistoryTable_m_a_g_i_c.getName()).getDatabaseName(), _ZezeHistoryTable_m_a_g_i_c);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_ZezeHistoryTable_m_a_g_i_c.getName()).getDatabaseName(), _ZezeHistoryTable_m_a_g_i_c);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    public void RegisterHttpServlet(Zeze.Netty.HttpServer httpServer) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        httpServer.addHandler("/Zeze/Builtin/HistoryModule/WalkPage", 8192,
                _reflect.getTransactionLevel("OnServletWalkPage", Zeze.Transaction.TransactionLevel.None),
                _reflect.getDispatchMode("OnServletWalkPage", Zeze.Transaction.DispatchMode.Normal),
                this::OnServletWalkPage);
    }

    protected abstract void OnServletWalkPage(Zeze.Netty.HttpExchange x) throws Exception;
}
