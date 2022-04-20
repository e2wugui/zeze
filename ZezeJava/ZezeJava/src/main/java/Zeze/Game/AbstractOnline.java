// auto-generated @formatter:off
package Zeze.Game;

public abstract class AbstractOnline extends Zeze.IModule {
    @Override public String getFullName() { return "Zeze.Beans.Game.Online"; }
    @Override public String getName() { return "Online"; }
    @Override public int getId() { return ModuleId; }
    public static final int ModuleId = 11013;

    public static final int RAlreadyExistRoleId = 1;
    public static final int RNotExistAccount = 2;
    public static final int RNotExistRoleId = 3;
    public static final int RNotLogin = 4;
    public static final int RNotLastLoginRoleId = 5;

    protected final Zeze.Beans.Game.Online.taccount _taccount = new Zeze.Beans.Game.Online.taccount();
    protected final Zeze.Beans.Game.Online.tonline _tonline = new Zeze.Beans.Game.Online.tonline();

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(this.getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Game.Online.Login>();
            factoryHandle.Factory = Zeze.Beans.Game.Online.Login::new;
            factoryHandle.Handle = this::ProcessLoginRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLoginRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47300786201153L, factoryHandle); // 11013, 311370305
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Game.Online.Logout>();
            factoryHandle.Factory = Zeze.Beans.Game.Online.Logout::new;
            factoryHandle.Handle = this::ProcessLogoutRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLogoutRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47302265853187L, factoryHandle); // 11013, 1791022339
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Game.Online.ReliableNotifyConfirm>();
            factoryHandle.Factory = Zeze.Beans.Game.Online.ReliableNotifyConfirm::new;
            factoryHandle.Handle = this::ProcessReliableNotifyConfirmRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReliableNotifyConfirmRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47303314651541L, factoryHandle); // 11013, -1455146603
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Game.Online.ReLogin>();
            factoryHandle.Factory = Zeze.Beans.Game.Online.ReLogin::new;
            factoryHandle.Handle = this::ProcessReLoginRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReLoginRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47304366001837L, factoryHandle); // 11013, -403796307
        }
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47300786201153L);
        service.getFactorys().remove(47302265853187L);
        service.getFactorys().remove(47303314651541L);
        service.getFactorys().remove(47304366001837L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.AddTable(zeze.getConfig().GetTableConf(_taccount.getName()).getDatabaseName(), _taccount);
        zeze.AddTable(zeze.getConfig().GetTableConf(_tonline.getName()).getDatabaseName(), _tonline);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_taccount.getName()).getDatabaseName(), _taccount);
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_tonline.getName()).getDatabaseName(), _tonline);
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessLoginRequest(Zeze.Beans.Game.Online.Login r) throws Throwable;
    protected abstract long ProcessLogoutRequest(Zeze.Beans.Game.Online.Logout r) throws Throwable;
    protected abstract long ProcessReliableNotifyConfirmRequest(Zeze.Beans.Game.Online.ReliableNotifyConfirm r) throws Throwable;
    protected abstract long ProcessReLoginRequest(Zeze.Beans.Game.Online.ReLogin r) throws Throwable;
}
