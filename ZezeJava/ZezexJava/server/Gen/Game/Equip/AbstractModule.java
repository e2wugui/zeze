// auto-generated @formatter:off
package Game.Equip;

import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantSuppression", "RedundantThrows", "SameReturnValue"})
public abstract class AbstractModule implements Zeze.IModule {
    public static final int ModuleId = 7;
    public static final String ModuleName = "Equip";
    public static final String ModuleFullName = "Game.Equip";

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

    protected abstract long ProcessEquipementRequest(Game.Equip.Equipement r) throws Exception;
    protected abstract long ProcessUnequipementRequest(Game.Equip.Unequipement r) throws Exception;
    protected abstract long ProcessReportLoginRequest(Game.Equip.ReportLogin r) throws Exception;

    protected final Game.Equip.tequip _tequip;
    protected final Game.Equip.tRelationalTest _tRelationalTest;

    public final Game.App App;

    public TableReadOnly<Long, Game.Equip.BEquips, Game.Equip.BEquipsReadOnly> getTequipReadOnly() {
        return _tequip;
    }

    public TableReadOnly<Long, Game.Equip.BRelational, Game.Equip.BRelationalReadOnly> getTRelationalTestReadOnly() {
        return _tRelationalTest;
    }

    public AbstractModule(Game.App app) {
        _tequip = new Game.Equip.tequip();
        _tRelationalTest = new Game.Equip.tRelationalTest();
        App = app;
        if (app != null)
            Register();
    }

    @Override
    public void Register() {
        // register protocol factory and handles
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Game.Equip.Equipement.class, Game.Equip.Equipement.TypeId_);
            factoryHandle.Factory = Game.Equip.Equipement::new;
            factoryHandle.Handle = this::ProcessEquipementRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessEquipementRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessEquipementRequest", Zeze.Transaction.DispatchMode.Normal);
            App.Server.AddFactoryHandle(32772485238L, factoryHandle); // 7, -1587253130
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Game.Equip.Unequipement.class, Game.Equip.Unequipement.TypeId_);
            factoryHandle.Factory = Game.Equip.Unequipement::new;
            factoryHandle.Handle = this::ProcessUnequipementRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnequipementRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUnequipementRequest", Zeze.Transaction.DispatchMode.Normal);
            App.Server.AddFactoryHandle(30979741226L, factoryHandle); // 7, 914970154
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Game.Equip.ReportLogin.class, Game.Equip.ReportLogin.TypeId_);
            factoryHandle.Factory = Game.Equip.ReportLogin::new;
            factoryHandle.Handle = this::ProcessReportLoginRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReportLoginRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReportLoginRequest", Zeze.Transaction.DispatchMode.Normal);
            App.Server.AddFactoryHandle(32195435014L, factoryHandle); // 7, 2130663942
        }
        // register table
        if (App.Zeze != null) {
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tequip.getName())).getDatabaseName(), _tequip);
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tRelationalTest.getName())).getDatabaseName(), _tRelationalTest);
        }
        // register servlet
    }

    @Override
    public void UnRegister() {
        // unregister protocol factory and handles
        App.Server.getFactorys().remove(32772485238L);
        App.Server.getFactorys().remove(30979741226L);
        App.Server.getFactorys().remove(32195435014L);
        // unregister table
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tequip.getName())).getDatabaseName(), _tequip);
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tRelationalTest.getName())).getDatabaseName(), _tRelationalTest);
        // unregister servlet
    }
}
