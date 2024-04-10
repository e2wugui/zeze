// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractZoker implements Zeze.IModule {
    public static final int ModuleId = 11037;
    public static final String ModuleName = "Zoker";
    public static final String ModuleFullName = "Zeze.Services.Zoker";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public static final int eDuplicateZoker = 1; // Zoker名字重复了
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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Zoker.AppendFile.class, Zeze.Builtin.Zoker.AppendFile.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Zoker.AppendFile::new;
            factoryHandle.Handle = this::ProcessAppendFileRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAppendFileRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAppendFileRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47406035711083L, factoryHandle); // 11037, -1813302165
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Zoker.CloseFile.class, Zeze.Builtin.Zoker.CloseFile.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Zoker.CloseFile::new;
            factoryHandle.Handle = this::ProcessCloseFileRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCloseFileRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCloseFileRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47406729836341L, factoryHandle); // 11037, -1119176907
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Zoker.CommitService.class, Zeze.Builtin.Zoker.CommitService.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Zoker.CommitService::new;
            factoryHandle.Handle = this::ProcessCommitServiceRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCommitServiceRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCommitServiceRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47406581820129L, factoryHandle); // 11037, -1267193119
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Zoker.ListSerivce.class, Zeze.Builtin.Zoker.ListSerivce.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Zoker.ListSerivce::new;
            factoryHandle.Handle = this::ProcessListSerivceRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessListSerivceRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessListSerivceRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47404082048889L, factoryHandle); // 11037, 528002937
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Zoker.OpenFile.class, Zeze.Builtin.Zoker.OpenFile.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Zoker.OpenFile::new;
            factoryHandle.Handle = this::ProcessOpenFileRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessOpenFileRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessOpenFileRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47405642508207L, factoryHandle); // 11037, 2088462255
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Zoker.Register.class, Zeze.Builtin.Zoker.Register.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Zoker.Register::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRegisterResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRegisterResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47407341877675L, factoryHandle); // 11037, -507135573
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Zoker.StartService.class, Zeze.Builtin.Zoker.StartService.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Zoker.StartService::new;
            factoryHandle.Handle = this::ProcessStartServiceRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessStartServiceRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessStartServiceRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47406220967437L, factoryHandle); // 11037, -1628045811
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Zoker.StopService.class, Zeze.Builtin.Zoker.StopService.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Zoker.StopService::new;
            factoryHandle.Handle = this::ProcessStopServiceRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessStopServiceRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessStopServiceRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47406262654700L, factoryHandle); // 11037, -1586358548
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47406035711083L);
        service.getFactorys().remove(47406729836341L);
        service.getFactorys().remove(47406581820129L);
        service.getFactorys().remove(47404082048889L);
        service.getFactorys().remove(47405642508207L);
        service.getFactorys().remove(47407341877675L);
        service.getFactorys().remove(47406220967437L);
        service.getFactorys().remove(47406262654700L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessAppendFileRequest(Zeze.Builtin.Zoker.AppendFile r) throws Exception;
    protected abstract long ProcessCloseFileRequest(Zeze.Builtin.Zoker.CloseFile r) throws Exception;
    protected abstract long ProcessCommitServiceRequest(Zeze.Builtin.Zoker.CommitService r) throws Exception;
    protected abstract long ProcessListSerivceRequest(Zeze.Builtin.Zoker.ListSerivce r) throws Exception;
    protected abstract long ProcessOpenFileRequest(Zeze.Builtin.Zoker.OpenFile r) throws Exception;
    protected abstract long ProcessStartServiceRequest(Zeze.Builtin.Zoker.StartService r) throws Exception;
    protected abstract long ProcessStopServiceRequest(Zeze.Builtin.Zoker.StopService r) throws Exception;
}
