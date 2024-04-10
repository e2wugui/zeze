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
    private final Zeze.Util.FastLock __thisLock = new Zeze.Util.FastLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }


    public static final int eOpenError = 1; // 打开文件发生了系统错误
    public static final int eAppendOffset = 2; // 添加数据时，Offset越界了（超出结尾）
    public static final int eCloseError = 3; // 关闭文件发生了系统错误
    public static final int eMd5Mismatch = 4; // 关闭文件时，验证md5失败
    public static final int eServiceOldExists = 5; // 发布更新服务时，发现备份目录存在
    public static final int eMoveOldFail = 6; // 发布更新服务时，备份失败
    public static final int eCommitFail = 7; // 发布更新服务时，发布服务失败

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.HotDistribute.AppendFile.class, Zeze.Builtin.HotDistribute.AppendFile.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.HotDistribute.AppendFile::new;
            factoryHandle.Handle = this::ProcessAppendFileRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAppendFileRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAppendFileRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47389309196186L, factoryHandle); // 11033, -1359947878
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.HotDistribute.CloseFile.class, Zeze.Builtin.HotDistribute.CloseFile.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.HotDistribute.CloseFile::new;
            factoryHandle.Handle = this::ProcessCloseFileRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCloseFileRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCloseFileRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47390444626489L, factoryHandle); // 11033, -224517575
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.HotDistribute.Commit.class, Zeze.Builtin.HotDistribute.Commit.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.HotDistribute.Commit::new;
            factoryHandle.Handle = this::ProcessCommitRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCommitRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCommitRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47390636330467L, factoryHandle); // 11033, -32813597
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.HotDistribute.GetLastVersionBeanInfo.class, Zeze.Builtin.HotDistribute.GetLastVersionBeanInfo.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.HotDistribute.GetLastVersionBeanInfo::new;
            factoryHandle.Handle = this::ProcessGetLastVersionBeanInfoRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessGetLastVersionBeanInfoRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessGetLastVersionBeanInfoRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47389512970537L, factoryHandle); // 11033, -1156173527
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.HotDistribute.OpenFile.class, Zeze.Builtin.HotDistribute.OpenFile.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.HotDistribute.OpenFile::new;
            factoryHandle.Handle = this::ProcessOpenFileRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessOpenFileRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessOpenFileRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47390201795475L, factoryHandle); // 11033, -467348589
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47389309196186L);
        service.getFactorys().remove(47390444626489L);
        service.getFactorys().remove(47390636330467L);
        service.getFactorys().remove(47389512970537L);
        service.getFactorys().remove(47390201795475L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessAppendFileRequest(Zeze.Builtin.HotDistribute.AppendFile r) throws Exception;
    protected abstract long ProcessCloseFileRequest(Zeze.Builtin.HotDistribute.CloseFile r) throws Exception;
    protected abstract long ProcessCommitRequest(Zeze.Builtin.HotDistribute.Commit r) throws Exception;
    protected abstract long ProcessGetLastVersionBeanInfoRequest(Zeze.Builtin.HotDistribute.GetLastVersionBeanInfo r) throws Exception;
    protected abstract long ProcessOpenFileRequest(Zeze.Builtin.HotDistribute.OpenFile r) throws Exception;
}
