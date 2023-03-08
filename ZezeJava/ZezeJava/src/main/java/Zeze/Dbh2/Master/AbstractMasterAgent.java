// auto-generated @formatter:off
package Zeze.Dbh2.Master;

public abstract class AbstractMasterAgent implements Zeze.IModule {
    public static final int ModuleId = 11027;
    public static final String ModuleName = "MasterAgent";
    public static final String ModuleFullName = "Zeze.Dbh2.Master.MasterAgent";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.GetBuckets.class, Zeze.Builtin.Dbh2.Master.GetBuckets.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.GetBuckets::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessGetBucketsResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessGetBucketsResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47363118214025L, factoryHandle); // 11027, -1781126263
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.LocateBucket.class, Zeze.Builtin.Dbh2.Master.LocateBucket.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.LocateBucket::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLocateBucketResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessLocateBucketResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47363709711447L, factoryHandle); // 11027, -1189628841
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47363118214025L);
        service.getFactorys().remove(47363709711447L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
