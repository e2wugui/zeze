// auto-generated @formatter:off
package Zeze.Hot;

public abstract class AbstractHotDistribute implements Zeze.IModule {
    public static final int ModuleId = 11033;
    public static final String ModuleName = "HotDistribute";
    public static final String ModuleFullName = "Zeze.Hot.HotDistribute";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.HotDistribute.GetLastVersionBeanInfo.class, Zeze.Builtin.HotDistribute.GetLastVersionBeanInfo.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.HotDistribute.GetLastVersionBeanInfo::new;
            factoryHandle.Handle = this::ProcessGetLastVersionBeanInfoRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessGetLastVersionBeanInfoRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessGetLastVersionBeanInfoRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47389512970537L, factoryHandle); // 11033, -1156173527
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47389512970537L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessGetLastVersionBeanInfoRequest(Zeze.Builtin.HotDistribute.GetLastVersionBeanInfo r) throws Exception;
}
