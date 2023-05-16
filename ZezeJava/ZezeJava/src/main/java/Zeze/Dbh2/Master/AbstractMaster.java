// auto-generated @formatter:off
package Zeze.Dbh2.Master;

public abstract class AbstractMaster implements Zeze.IModule {
    public static final int ModuleId = 11027;
    public static final String ModuleName = "Master";
    public static final String ModuleFullName = "Zeze.Dbh2.Master.Master";

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
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCreateBucketResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCreateBucketResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47364327162209L, factoryHandle); // 11027, -572178079
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.CreateDatabase.class, Zeze.Builtin.Dbh2.Master.CreateDatabase.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.CreateDatabase::new;
            factoryHandle.Handle = this::ProcessCreateDatabaseRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCreateDatabaseRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCreateDatabaseRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47361973054464L, factoryHandle); // 11027, 1368681472
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.CreateSplitBucket.class, Zeze.Builtin.Dbh2.Master.CreateSplitBucket.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.CreateSplitBucket::new;
            factoryHandle.Handle = this::ProcessCreateSplitBucketRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCreateSplitBucketRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCreateSplitBucketRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47362664777370L, factoryHandle); // 11027, 2060404378
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.CreateTable.class, Zeze.Builtin.Dbh2.Master.CreateTable.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.CreateTable::new;
            factoryHandle.Handle = this::ProcessCreateTableRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCreateTableRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCreateTableRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47363344664675L, factoryHandle); // 11027, -1554675613
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.EndSplit.class, Zeze.Builtin.Dbh2.Master.EndSplit.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.EndSplit::new;
            factoryHandle.Handle = this::ProcessEndSplitRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessEndSplitRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessEndSplitRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47363904457956L, factoryHandle); // 11027, -994882332
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.GetBuckets.class, Zeze.Builtin.Dbh2.Master.GetBuckets.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.GetBuckets::new;
            factoryHandle.Handle = this::ProcessGetBucketsRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessGetBucketsRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessGetBucketsRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47363118214025L, factoryHandle); // 11027, -1781126263
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.LocateBucket.class, Zeze.Builtin.Dbh2.Master.LocateBucket.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.LocateBucket::new;
            factoryHandle.Handle = this::ProcessLocateBucketRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLocateBucketRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessLocateBucketRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47363709711447L, factoryHandle); // 11027, -1189628841
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.Register.class, Zeze.Builtin.Dbh2.Master.Register.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.Register::new;
            factoryHandle.Handle = this::ProcessRegisterRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRegisterRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRegisterRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47364347310157L, factoryHandle); // 11027, -552030131
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.ReportBucketCount.class, Zeze.Builtin.Dbh2.Master.ReportBucketCount.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.ReportBucketCount::new;
            factoryHandle.Handle = this::ProcessReportBucketCountRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReportBucketCountRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReportBucketCountRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47360684865688L, factoryHandle); // 11027, 80492696
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.ReportLoad.class, Zeze.Builtin.Dbh2.Master.ReportLoad.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.ReportLoad::new;
            factoryHandle.Handle = this::ProcessReportLoadRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReportLoadRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReportLoadRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47363711595808L, factoryHandle); // 11027, -1187744480
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47364327162209L);
        service.getFactorys().remove(47361973054464L);
        service.getFactorys().remove(47362664777370L);
        service.getFactorys().remove(47363344664675L);
        service.getFactorys().remove(47363904457956L);
        service.getFactorys().remove(47363118214025L);
        service.getFactorys().remove(47363709711447L);
        service.getFactorys().remove(47364347310157L);
        service.getFactorys().remove(47360684865688L);
        service.getFactorys().remove(47363711595808L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessCreateDatabaseRequest(Zeze.Builtin.Dbh2.Master.CreateDatabase r) throws Exception;
    protected abstract long ProcessCreateSplitBucketRequest(Zeze.Builtin.Dbh2.Master.CreateSplitBucket r) throws Exception;
    protected abstract long ProcessCreateTableRequest(Zeze.Builtin.Dbh2.Master.CreateTable r) throws Exception;
    protected abstract long ProcessEndSplitRequest(Zeze.Builtin.Dbh2.Master.EndSplit r) throws Exception;
    protected abstract long ProcessGetBucketsRequest(Zeze.Builtin.Dbh2.Master.GetBuckets r) throws Exception;
    protected abstract long ProcessLocateBucketRequest(Zeze.Builtin.Dbh2.Master.LocateBucket r) throws Exception;
    protected abstract long ProcessRegisterRequest(Zeze.Builtin.Dbh2.Master.Register r) throws Exception;
    protected abstract long ProcessReportBucketCountRequest(Zeze.Builtin.Dbh2.Master.ReportBucketCount r) throws Exception;
    protected abstract long ProcessReportLoadRequest(Zeze.Builtin.Dbh2.Master.ReportLoad r) throws Exception;
}
