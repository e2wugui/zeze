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

    public static final int eDatabaseNotFound = 1;
    public static final int eTableNotFound = 2;
    public static final int eTableIsNew = 3;
    public static final int eSplittingBucketNotFound = 4;
    public static final int eManagerNotFound = 5;
    public static final int eSplittingBucketExist = 6;
    public static final int eTooFewManager = 7;

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.CreateBucket.class, Zeze.Builtin.Dbh2.Master.CreateBucket.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.CreateBucket::new;
            factoryHandle.Handle = this::ProcessCreateBucketRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCreateBucketRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCreateBucketRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47364327162209L, factoryHandle); // 11027, -572178079
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.CreateDatabase.class, Zeze.Builtin.Dbh2.Master.CreateDatabase.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.CreateDatabase::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCreateDatabaseResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCreateDatabaseResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47361973054464L, factoryHandle); // 11027, 1368681472
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.CreateSplitBucket.class, Zeze.Builtin.Dbh2.Master.CreateSplitBucket.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.CreateSplitBucket::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCreateSplitBucketResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCreateSplitBucketResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47362664777370L, factoryHandle); // 11027, 2060404378
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.CreateTable.class, Zeze.Builtin.Dbh2.Master.CreateTable.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.CreateTable::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCreateTableResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCreateTableResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47363344664675L, factoryHandle); // 11027, -1554675613
        }
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
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.Register.class, Zeze.Builtin.Dbh2.Master.Register.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.Register::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRegisterResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRegisterResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47364347310157L, factoryHandle); // 11027, -552030131
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.ReportLoad.class, Zeze.Builtin.Dbh2.Master.ReportLoad.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.ReportLoad::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReportLoadResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReportLoadResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47363711595808L, factoryHandle); // 11027, -1187744480
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47364327162209L);
        service.getFactorys().remove(47361973054464L);
        service.getFactorys().remove(47362664777370L);
        service.getFactorys().remove(47363344664675L);
        service.getFactorys().remove(47363118214025L);
        service.getFactorys().remove(47363709711447L);
        service.getFactorys().remove(47364347310157L);
        service.getFactorys().remove(47363711595808L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessCreateBucketRequest(Zeze.Builtin.Dbh2.Master.CreateBucket r) throws Exception;
}
