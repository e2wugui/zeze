// auto-generated @formatter:off
package Zeze.Game;

public abstract class AbstractOnline implements Zeze.IModule {
    public static final int ModuleId = 11013;
    public static final String ModuleName = "Online";
    public static final String ModuleFullName = "Zeze.Game.Online";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    public static final int ResultCodeSuccess = 0;
    public static final int ResultCodeCreateRoleDuplicateRoleName = 1;
    public static final int ResultCodeAccountNotExist = 2;
    public static final int ResultCodeRoleNotExist = 3;
    public static final int ResultCodeNotLastLoginRoleId = 4;
    public static final int ResultCodeOnlineDataNotFound = 5;
    public static final int ResultCodeReliableNotifyConfirmIndexOutOfRange = 6;
    public static final int ResultCodeNotLogin = 7;

    protected String multiInstanceName = "";

    protected final Zeze.Builtin.Game.Online.tlocal _tlocal = new Zeze.Builtin.Game.Online.tlocal();
    protected final Zeze.Builtin.Game.Online.tonline _tonline = new Zeze.Builtin.Game.Online.tonline();
    protected final Zeze.Builtin.Game.Online.tversion _tversion = new Zeze.Builtin.Game.Online.tversion();

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Game.Online.Login.class, Zeze.Builtin.Game.Online.Login.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Game.Online.Login::new;
            factoryHandle.Handle = this::ProcessLoginRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLoginRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessLoginRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47303980222879L, factoryHandle); // 11013, -789575265
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Game.Online.Logout.class, Zeze.Builtin.Game.Online.Logout.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Game.Online.Logout::new;
            factoryHandle.Handle = this::ProcessLogoutRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLogoutRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessLogoutRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47304205955457L, factoryHandle); // 11013, -563842687
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Game.Online.ReliableNotifyConfirm.class, Zeze.Builtin.Game.Online.ReliableNotifyConfirm.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Game.Online.ReliableNotifyConfirm::new;
            factoryHandle.Handle = this::ProcessReliableNotifyConfirmRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReliableNotifyConfirmRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReliableNotifyConfirmRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47304349755660L, factoryHandle); // 11013, -420042484
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Game.Online.ReLogin.class, Zeze.Builtin.Game.Online.ReLogin.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Game.Online.ReLogin::new;
            factoryHandle.Handle = this::ProcessReLoginRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReLoginRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReLoginRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47304551116333L, factoryHandle); // 11013, -218681811
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47303980222879L);
        service.getFactorys().remove(47304205955457L);
        service.getFactorys().remove(47304349755660L);
        service.getFactorys().remove(47304551116333L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tlocal.getName()).getDatabaseName(), _tlocal);
        zeze.addTable(zeze.getConfig().getTableConf(_tonline.getName()).getDatabaseName(), _tonline);
        zeze.addTable(zeze.getConfig().getTableConf(_tversion.getName()).getDatabaseName(), _tversion);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tlocal.getName()).getDatabaseName(), _tlocal);
        zeze.removeTable(zeze.getConfig().getTableConf(_tonline.getName()).getDatabaseName(), _tonline);
        zeze.removeTable(zeze.getConfig().getTableConf(_tversion.getName()).getDatabaseName(), _tversion);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessLoginRequest(Zeze.Builtin.Game.Online.Login r) throws Exception;
    protected abstract long ProcessLogoutRequest(Zeze.Builtin.Game.Online.Logout r) throws Exception;
    protected abstract long ProcessReliableNotifyConfirmRequest(Zeze.Builtin.Game.Online.ReliableNotifyConfirm r) throws Exception;
    protected abstract long ProcessReLoginRequest(Zeze.Builtin.Game.Online.ReLogin r) throws Exception;
}
