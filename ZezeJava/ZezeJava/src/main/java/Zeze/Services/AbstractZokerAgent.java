// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractZokerAgent implements Zeze.IModule {
    public static final int ModuleId = 11037;
    public static final String ModuleName = "ZokerAgent";
    public static final String ModuleFullName = "Zeze.Services.ZokerAgent";

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
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAppendFileResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAppendFileResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47406035711083L, factoryHandle); // 11037, -1813302165
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Zoker.CloseFile.class, Zeze.Builtin.Zoker.CloseFile.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Zoker.CloseFile::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCloseFileResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCloseFileResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47406729836341L, factoryHandle); // 11037, -1119176907
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Zoker.CommitService.class, Zeze.Builtin.Zoker.CommitService.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Zoker.CommitService::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCommitServiceResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCommitServiceResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47406581820129L, factoryHandle); // 11037, -1267193119
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Zoker.ListSerivce.class, Zeze.Builtin.Zoker.ListSerivce.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Zoker.ListSerivce::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessListSerivceResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessListSerivceResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47404082048889L, factoryHandle); // 11037, 528002937
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Zoker.OpenFile.class, Zeze.Builtin.Zoker.OpenFile.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Zoker.OpenFile::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessOpenFileResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessOpenFileResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47405642508207L, factoryHandle); // 11037, 2088462255
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Zoker.Register.class, Zeze.Builtin.Zoker.Register.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Zoker.Register::new;
            factoryHandle.Handle = this::ProcessRegisterRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRegisterRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRegisterRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47407341877675L, factoryHandle); // 11037, -507135573
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Zoker.StartService.class, Zeze.Builtin.Zoker.StartService.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Zoker.StartService::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessStartServiceResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessStartServiceResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47406220967437L, factoryHandle); // 11037, -1628045811
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Zoker.StopService.class, Zeze.Builtin.Zoker.StopService.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Zoker.StopService::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessStopServiceResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessStopServiceResponse", Zeze.Transaction.DispatchMode.Normal);
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

    protected abstract long ProcessRegisterRequest(Zeze.Builtin.Zoker.Register r) throws Exception;
}
