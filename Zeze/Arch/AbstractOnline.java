// auto-generated @formatter:off
package Zeze.Arch;

public abstract class AbstractOnline extends Zeze.IModule {
    public static final int ModuleId = 11100;
    @Override public String getFullName() { return "Zeze.Arch.Online"; }
    @Override public String getName() { return "Online"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    public static final int ResultCodeSuccess = 0;
    public static final int ResultCodeCreateRoleDuplicateRoleName = 1;
    public static final int ResultCodeAccountNotExist = 2;
    public static final int ResultCodeRoleNotExist = 3;
    public static final int ResultCodeNotLastLoginRoleId = 4;
    public static final int ResultCodeOnlineDataNotFound = 5;
    public static final int ResultCodeReliableNotifyConfirmCountOutOfRange = 6;
    public static final int ResultCodeNotLogin = 7;

    protected final Zeze.Builtin.Online.taccount _taccount = new Zeze.Builtin.Online.taccount();
    protected final Zeze.Builtin.Online.tlocal _tlocal = new Zeze.Builtin.Online.tlocal();
    protected final Zeze.Builtin.Online.tonline _tonline = new Zeze.Builtin.Online.tonline();

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(this.getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Online.Login>();
            factoryHandle.Factory = Zeze.Builtin.Online.Login::new;
            factoryHandle.Handle = this::ProcessLoginRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLoginRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47676933001134L, factoryHandle); // 11100, -1498951762
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Online.Logout>();
            factoryHandle.Factory = Zeze.Builtin.Online.Logout::new;
            factoryHandle.Handle = this::ProcessLogoutRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLogoutRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47676519983553L, factoryHandle); // 11100, -1911969343
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Online.ReliableNotifyConfirm>();
            factoryHandle.Factory = Zeze.Builtin.Online.ReliableNotifyConfirm::new;
            factoryHandle.Handle = this::ProcessReliableNotifyConfirmRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReliableNotifyConfirmRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47678187220010L, factoryHandle); // 11100, -244732886
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Online.ReLogin>();
            factoryHandle.Factory = Zeze.Builtin.Online.ReLogin::new;
            factoryHandle.Handle = this::ProcessReLoginRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReLoginRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47675064884515L, factoryHandle); // 11100, 927898915
        }
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47676933001134L);
        service.getFactorys().remove(47676519983553L);
        service.getFactorys().remove(47678187220010L);
        service.getFactorys().remove(47675064884515L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.AddTable(zeze.getConfig().GetTableConf(_taccount.getName()).getDatabaseName(), _taccount);
        zeze.AddTable(zeze.getConfig().GetTableConf(_tlocal.getName()).getDatabaseName(), _tlocal);
        zeze.AddTable(zeze.getConfig().GetTableConf(_tonline.getName()).getDatabaseName(), _tonline);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_taccount.getName()).getDatabaseName(), _taccount);
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_tlocal.getName()).getDatabaseName(), _tlocal);
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_tonline.getName()).getDatabaseName(), _tonline);
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessLoginRequest(Zeze.Builtin.Online.Login r) throws Throwable;
    protected abstract long ProcessLogoutRequest(Zeze.Builtin.Online.Logout r) throws Throwable;
    protected abstract long ProcessReliableNotifyConfirmRequest(Zeze.Builtin.Online.ReliableNotifyConfirm r) throws Throwable;
    protected abstract long ProcessReLoginRequest(Zeze.Builtin.Online.ReLogin r) throws Throwable;
}
