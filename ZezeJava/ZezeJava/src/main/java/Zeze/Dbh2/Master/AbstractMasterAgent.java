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

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.CheckFreeManager.class, Zeze.Builtin.Dbh2.Master.CheckFreeManager.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.CheckFreeManager::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCheckFreeManagerResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCheckFreeManagerResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47364210591783L, factoryHandle); // 11027, -688748505
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.ClearInUse.class, Zeze.Builtin.Dbh2.Master.ClearInUse.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.ClearInUse::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessClearInUseResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessClearInUseResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47363354868451L, factoryHandle); // 11027, -1544471837
        }
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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.EndMove.class, Zeze.Builtin.Dbh2.Master.EndMove.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.EndMove::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessEndMoveResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessEndMoveResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47362660475482L, factoryHandle); // 11027, 2056102490
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.EndSplit.class, Zeze.Builtin.Dbh2.Master.EndSplit.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.EndSplit::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessEndSplitResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessEndSplitResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47363904457956L, factoryHandle); // 11027, -994882332
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.GetBuckets.class, Zeze.Builtin.Dbh2.Master.GetBuckets.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.GetBuckets::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessGetBucketsResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessGetBucketsResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47363118214025L, factoryHandle); // 11027, -1781126263
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.GetDataWithVersion.class, Zeze.Builtin.Dbh2.Master.GetDataWithVersion.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.GetDataWithVersion::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessGetDataWithVersionResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessGetDataWithVersionResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47361193774107L, factoryHandle); // 11027, 589401115
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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.ReportBucketCount.class, Zeze.Builtin.Dbh2.Master.ReportBucketCount.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.ReportBucketCount::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReportBucketCountResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReportBucketCountResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47360684865688L, factoryHandle); // 11027, 80492696
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.ReportLoad.class, Zeze.Builtin.Dbh2.Master.ReportLoad.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.ReportLoad::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReportLoadResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReportLoadResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47363711595808L, factoryHandle); // 11027, -1187744480
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.SaveDataWithSameVersion.class, Zeze.Builtin.Dbh2.Master.SaveDataWithSameVersion.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.SaveDataWithSameVersion::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSaveDataWithSameVersionResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSaveDataWithSameVersionResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47363401603908L, factoryHandle); // 11027, -1497736380
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.SetInUse.class, Zeze.Builtin.Dbh2.Master.SetInUse.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.SetInUse::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSetInUseResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSetInUseResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47360856379529L, factoryHandle); // 11027, 252006537
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.TryLock.class, Zeze.Builtin.Dbh2.Master.TryLock.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.TryLock::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessTryLockResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessTryLockResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47363396832137L, factoryHandle); // 11027, -1502508151
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.UnLock.class, Zeze.Builtin.Dbh2.Master.UnLock.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.UnLock::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnLockResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUnLockResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47362815788753L, factoryHandle); // 11027, -2083551535
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47364210591783L);
        service.getFactorys().remove(47363354868451L);
        service.getFactorys().remove(47364327162209L);
        service.getFactorys().remove(47361973054464L);
        service.getFactorys().remove(47362664777370L);
        service.getFactorys().remove(47363344664675L);
        service.getFactorys().remove(47362660475482L);
        service.getFactorys().remove(47363904457956L);
        service.getFactorys().remove(47363118214025L);
        service.getFactorys().remove(47361193774107L);
        service.getFactorys().remove(47363709711447L);
        service.getFactorys().remove(47364347310157L);
        service.getFactorys().remove(47360684865688L);
        service.getFactorys().remove(47363711595808L);
        service.getFactorys().remove(47363401603908L);
        service.getFactorys().remove(47360856379529L);
        service.getFactorys().remove(47363396832137L);
        service.getFactorys().remove(47362815788753L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessCreateBucketRequest(Zeze.Builtin.Dbh2.Master.CreateBucket r) throws Exception;
}
