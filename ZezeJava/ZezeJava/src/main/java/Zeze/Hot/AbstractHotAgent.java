// auto-generated @formatter:off
package Zeze.Hot;

public abstract class AbstractHotAgent implements Zeze.IModule {
    public static final int ModuleId = 11033;
    public static final String ModuleName = "HotAgent";
    public static final String ModuleFullName = "Zeze.Hot.HotAgent";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public static final int eOpenError = 1; // 打开文件发生了系统错误
    public static final int eAppendOffset = 2; // 添加数据时，Offset越界了（超出结尾）
    public static final int eCloseError = 3; // 关闭文件发生了系统错误
    public static final int eMd5Mismatch = 4; // 关闭文件时，验证md5失败
    public static final int eServiceOldExists = 5; // 发布更新服务时，发现备份目录存在
    public static final int eMoveOldFail = 6; // 发布更新服务时，备份失败
    public static final int eIdle = 0;
    public static final int ePrepare = 7;
    public static final int eTryDistribute = 8;
    public static final int eCommit = 9;
    public static final int eCommit2 = 10;
    public static final int eDistributeIdMismatch = 11;
    public static final int eTryRollback = 12;
    public static final int eInstall = 13;

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.HotDistribute.AppendFile.class, Zeze.Builtin.HotDistribute.AppendFile.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.HotDistribute.AppendFile::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAppendFileResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAppendFileResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47389309196186L, factoryHandle); // 11033, -1359947878
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.HotDistribute.CloseFile.class, Zeze.Builtin.HotDistribute.CloseFile.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.HotDistribute.CloseFile::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCloseFileResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCloseFileResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47390444626489L, factoryHandle); // 11033, -224517575
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.HotDistribute.Commit.class, Zeze.Builtin.HotDistribute.Commit.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.HotDistribute.Commit::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCommitResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCommitResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47390636330467L, factoryHandle); // 11033, -32813597
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.HotDistribute.Commit2.class, Zeze.Builtin.HotDistribute.Commit2.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.HotDistribute.Commit2::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCommit2Response", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCommit2Response", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47387265579966L, factoryHandle); // 11033, 891403198
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.HotDistribute.GetLastVersionBeanInfo.class, Zeze.Builtin.HotDistribute.GetLastVersionBeanInfo.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.HotDistribute.GetLastVersionBeanInfo::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessGetLastVersionBeanInfoResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessGetLastVersionBeanInfoResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47389512970537L, factoryHandle); // 11033, -1156173527
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.HotDistribute.OpenFile.class, Zeze.Builtin.HotDistribute.OpenFile.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.HotDistribute.OpenFile::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessOpenFileResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessOpenFileResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47390201795475L, factoryHandle); // 11033, -467348589
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.HotDistribute.PrepareDistribute.class, Zeze.Builtin.HotDistribute.PrepareDistribute.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.HotDistribute.PrepareDistribute::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessPrepareDistributeResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessPrepareDistributeResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47389258998777L, factoryHandle); // 11033, -1410145287
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.HotDistribute.TryDistribute.class, Zeze.Builtin.HotDistribute.TryDistribute.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.HotDistribute.TryDistribute::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessTryDistributeResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessTryDistributeResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47388449680241L, factoryHandle); // 11033, 2075503473
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.HotDistribute.TryRollback.class, Zeze.Builtin.HotDistribute.TryRollback.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.HotDistribute.TryRollback::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessTryRollbackResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessTryRollbackResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47386455650973L, factoryHandle); // 11033, 81474205
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47389309196186L);
        service.getFactorys().remove(47390444626489L);
        service.getFactorys().remove(47390636330467L);
        service.getFactorys().remove(47387265579966L);
        service.getFactorys().remove(47389512970537L);
        service.getFactorys().remove(47390201795475L);
        service.getFactorys().remove(47389258998777L);
        service.getFactorys().remove(47388449680241L);
        service.getFactorys().remove(47386455650973L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
