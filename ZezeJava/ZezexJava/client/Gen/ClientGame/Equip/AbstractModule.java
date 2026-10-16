// auto-generated @formatter:off
package ClientGame.Equip;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantSuppression", "RedundantThrows", "SameReturnValue"})
public abstract class AbstractModule implements Zeze.IModule {
    public static final int ModuleId = 7;
    public static final String ModuleName = "Equip";
    public static final String ModuleFullName = "ClientGame.Equip";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public static final int ResultCodeCannotEquip = 1;
    public static final int ResultCodeItemNotFound = 2;
    public static final int ResultCodeBagIsFull = 3;
    public static final int ResultCodeEquipNotFound = 4;

    protected abstract long ProcessSEquipement(ClientGame.Equip.SEquipement p) throws Exception;

    public final ClientGame.App App;

    public AbstractModule(ClientGame.App app) {
        App = app;
        if (app != null)
            Register();
    }

    @Override
    public void Register() {
        // register protocol factory and handles
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(ClientGame.Equip.Equipement.class, ClientGame.Equip.Equipement.TypeId_);
            factoryHandle.Factory = ClientGame.Equip.Equipement::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessEquipementResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessEquipementResponse", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(32772485238L, factoryHandle); // 7, -1587253130
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(ClientGame.Equip.Unequipement.class, ClientGame.Equip.Unequipement.TypeId_);
            factoryHandle.Factory = ClientGame.Equip.Unequipement::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnequipementResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUnequipementResponse", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(30979741226L, factoryHandle); // 7, 914970154
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(ClientGame.Equip.SEquipement.class, ClientGame.Equip.SEquipement.TypeId_);
            factoryHandle.Factory = ClientGame.Equip.SEquipement::new;
            factoryHandle.Handle = this::ProcessSEquipement;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSEquipement", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSEquipement", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(31296158603L, factoryHandle); // 7, 1231387531
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(ClientGame.Equip.ReportLogin.class, ClientGame.Equip.ReportLogin.TypeId_);
            factoryHandle.Factory = ClientGame.Equip.ReportLogin::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReportLoginResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReportLoginResponse", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(32195435014L, factoryHandle); // 7, 2130663942
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(ClientGame.Equip.SendHot.class, ClientGame.Equip.SendHot.TypeId_);
            factoryHandle.Factory = ClientGame.Equip.SendHot::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSendHotResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSendHotResponse", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(30506682560L, factoryHandle); // 7, 441911488
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(ClientGame.Equip.SendHotRemove.class, ClientGame.Equip.SendHotRemove.TypeId_);
            factoryHandle.Factory = ClientGame.Equip.SendHotRemove::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSendHotRemoveResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSendHotRemoveResponse", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(31688523471L, factoryHandle); // 7, 1623752399
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(ClientGame.Equip.SendHotAdd.class, ClientGame.Equip.SendHotAdd.TypeId_);
            factoryHandle.Factory = ClientGame.Equip.SendHotAdd::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSendHotAddResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSendHotAddResponse", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(30316107850L, factoryHandle); // 7, 251336778
        }
        // register table
        // register servlet
    }

    @Override
    public void UnRegister() {
        // unregister protocol factory and handles
        App.ClientService.getFactorys().remove(32772485238L);
        App.ClientService.getFactorys().remove(30979741226L);
        App.ClientService.getFactorys().remove(31296158603L);
        App.ClientService.getFactorys().remove(32195435014L);
        App.ClientService.getFactorys().remove(30506682560L);
        App.ClientService.getFactorys().remove(31688523471L);
        App.ClientService.getFactorys().remove(30316107850L);
        // unregister table
        // unregister servlet
    }
}
