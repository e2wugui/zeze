// auto-generated @formatter:off
package demo.Module1;

// module的注释
@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantSuppression", "RedundantThrows", "SameReturnValue"})
public abstract class AbstractModule implements Zeze.IModule {
    public static final int ModuleId = 1;
    public static final String ModuleName = "Module1";
    public static final String ModuleFullName = "demo.Module1";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    protected abstract long ProcessProtocol4(demo.Module1.Protocol4 p) throws Exception;
    protected abstract long ProcessProtocol3(demo.Module1.Protocol3 p) throws Exception;
    protected abstract long ProcessRpc1Request(demo.Module1.Rpc1 r) throws Exception;
    protected abstract long ProcessProtocolNoProcedure(demo.Module1.ProtocolNoProcedure p) throws Exception;
    protected abstract long ProcessProtocolUseData(demo.Module1.ProtocolUseData p) throws Exception;
    protected abstract long ProcessProtocolOnlyUseData(demo.Module1.ProtocolOnlyUseData p) throws Exception;
    protected abstract long ProcessProtocolOnlyData(demo.Module1.ProtocolOnlyData p) throws Exception;
    protected abstract long ProcessRpcOnlyUseDataRequest(demo.Module1.RpcOnlyUseData r) throws Exception;
    protected abstract long ProcessRpcOnlyDataRequest(demo.Module1.RpcOnlyData r) throws Exception;

    protected final demo.Module1.Table1 _Table1;
    protected final demo.Module1.Table2 _Table2;
    protected final demo.Module1.Table3 _Table3;
    protected final demo.Module1.Table5 _Table5;
    protected final demo.Module1.TableImportant _TableImportant;
    protected final demo.Module1.tAutokey _tAutokey;
    protected final demo.Module1.tAutoKeyRandom _tAutoKeyRandom;
    protected final demo.Module1.tCoverHistory _tCoverHistory;
    protected final demo.Module1.tflush _tflush;
    protected final demo.Module1.tKuafu _tKuafu;
    protected final demo.Module1.tMemorySize _tMemorySize;
    protected final demo.Module1.tTestSchemas _tTestSchemas;
    protected final demo.Module1.tWalkPage _tWalkPage;

    public final demo.App App;

    public AbstractModule(demo.App app) {
        var appMainVersion = app.Zeze.getConfig().getAppMainVersion();
        var appMainVersionSuffix = appMainVersion != 0 ? "__" + appMainVersion : null;
        var serverIdSuffix = String.valueOf(app.Zeze.getConfig().getServerId());
        _Table1 = new demo.Module1.Table1();
        _Table2 = new demo.Module1.Table2();
        _Table3 = new demo.Module1.Table3();
        _Table5 = new demo.Module1.Table5();
        _TableImportant = new demo.Module1.TableImportant();
        _tAutokey = new demo.Module1.tAutokey();
        _tAutoKeyRandom = new demo.Module1.tAutoKeyRandom();
        _tCoverHistory = new demo.Module1.tCoverHistory();
        _tflush = new demo.Module1.tflush();
        _tKuafu = new demo.Module1.tKuafu(appMainVersionSuffix);
        _tMemorySize = new demo.Module1.tMemorySize();
        _tTestSchemas = new demo.Module1.tTestSchemas(serverIdSuffix);
        _tWalkPage = new demo.Module1.tWalkPage();
        App = app;
        if (app != null)
            Register();
    }

    @Override
    public void Register() {
        // register protocol factory and handles
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(demo.Module1.Protocol4.class, demo.Module1.Protocol4.TypeId_);
            factoryHandle.Factory = demo.Module1.Protocol4::new;
            factoryHandle.Handle = this::ProcessProtocol4;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessProtocol4", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessProtocol4", Zeze.Transaction.DispatchMode.Normal);
            App.TestServer.AddFactoryHandle(5222864529L, factoryHandle); // 1, 927897233
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(demo.Module1.Protocol3.class, demo.Module1.Protocol3.TypeId_);
            factoryHandle.Factory = demo.Module1.Protocol3::new;
            factoryHandle.Handle = this::ProcessProtocol3;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessProtocol3", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessProtocol3", Zeze.Transaction.DispatchMode.Normal);
            factoryHandle.CriticalLevel = Zeze.Net.Protocol.eCritical;
            App.TestServer.AddFactoryHandle(7815467220L, factoryHandle); // 1, -774467372
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(demo.Module1.Rpc1.class, demo.Module1.Rpc1.TypeId_);
            factoryHandle.Factory = demo.Module1.Rpc1::new;
            factoryHandle.Handle = this::ProcessRpc1Request;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRpc1Request", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRpc1Request", Zeze.Transaction.DispatchMode.Normal);
            App.TestServer.AddFactoryHandle(5635082623L, factoryHandle); // 1, 1340115327
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(demo.Module1.Rpc2.class, demo.Module1.Rpc2.TypeId_);
            factoryHandle.Factory = demo.Module1.Rpc2::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRpc2Response", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRpc2Response", Zeze.Transaction.DispatchMode.Normal);
            factoryHandle.CriticalLevel = Zeze.Net.Protocol.eNormal;
            App.TestServer.AddFactoryHandle(7854078040L, factoryHandle); // 1, -735856552
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(demo.Module1.ProtocolNoProcedure.class, demo.Module1.ProtocolNoProcedure.TypeId_);
            factoryHandle.Factory = demo.Module1.ProtocolNoProcedure::new;
            factoryHandle.Handle = this::ProcessProtocolNoProcedure;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessProtocolNoProcedure", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessProtocolNoProcedure", Zeze.Transaction.DispatchMode.Normal);
            App.TestServer.AddFactoryHandle(5645907125L, factoryHandle); // 1, 1350939829
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(demo.Module1.ProtocolUseData.class, demo.Module1.ProtocolUseData.TypeId_);
            factoryHandle.Factory = demo.Module1.ProtocolUseData::new;
            factoryHandle.Handle = this::ProcessProtocolUseData;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessProtocolUseData", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessProtocolUseData", Zeze.Transaction.DispatchMode.Normal);
            factoryHandle.CriticalLevel = Zeze.Net.Protocol.eSheddable;
            App.TestServer.AddFactoryHandle(6379586904L, factoryHandle); // 1, 2084619608
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(demo.Module1.ProtocolOnlyUseData.class, demo.Module1.ProtocolOnlyUseData.TypeId_);
            factoryHandle.Factory = demo.Module1.ProtocolOnlyUseData::new;
            factoryHandle.Handle = this::ProcessProtocolOnlyUseData;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessProtocolOnlyUseData", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessProtocolOnlyUseData", Zeze.Transaction.DispatchMode.Normal);
            App.TestServer.AddFactoryHandle(7562655298L, factoryHandle); // 1, -1027279294
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(demo.Module1.ProtocolOnlyData.class, demo.Module1.ProtocolOnlyData.TypeId_);
            factoryHandle.Factory = demo.Module1.ProtocolOnlyData::new;
            factoryHandle.Handle = this::ProcessProtocolOnlyData;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessProtocolOnlyData", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessProtocolOnlyData", Zeze.Transaction.DispatchMode.Normal);
            App.TestServer.AddFactoryHandle(6671907719L, factoryHandle); // 1, -1918026873
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(demo.Module1.RpcOnlyUseData.class, demo.Module1.RpcOnlyUseData.TypeId_);
            factoryHandle.Factory = demo.Module1.RpcOnlyUseData::new;
            factoryHandle.Handle = this::ProcessRpcOnlyUseDataRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRpcOnlyUseDataRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRpcOnlyUseDataRequest", Zeze.Transaction.DispatchMode.Normal);
            App.TestServer.AddFactoryHandle(7938190671L, factoryHandle); // 1, -651743921
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(demo.Module1.RpcOnlyData.class, demo.Module1.RpcOnlyData.TypeId_);
            factoryHandle.Factory = demo.Module1.RpcOnlyData::new;
            factoryHandle.Handle = this::ProcessRpcOnlyDataRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRpcOnlyDataRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRpcOnlyDataRequest", Zeze.Transaction.DispatchMode.Normal);
            App.TestServer.AddFactoryHandle(7336162357L, factoryHandle); // 1, -1253772235
        }
        // register table
        if (App.Zeze != null) {
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_Table1.getName())).getDatabaseName(), _Table1);
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_Table2.getName())).getDatabaseName(), _Table2);
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_Table3.getName())).getDatabaseName(), _Table3);
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_Table5.getName())).getDatabaseName(), _Table5);
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_TableImportant.getName())).getDatabaseName(), _TableImportant);
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tAutokey.getName())).getDatabaseName(), _tAutokey);
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tAutoKeyRandom.getName())).getDatabaseName(), _tAutoKeyRandom);
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tCoverHistory.getName())).getDatabaseName(), _tCoverHistory);
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tflush.getName())).getDatabaseName(), _tflush);
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tKuafu.getName())).getDatabaseName(), _tKuafu);
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tMemorySize.getName())).getDatabaseName(), _tMemorySize);
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tTestSchemas.getName())).getDatabaseName(), _tTestSchemas);
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tWalkPage.getName())).getDatabaseName(), _tWalkPage);
        }
        // register servlet
    }

    @Override
    public void UnRegister() {
        // unregister protocol factory and handles
        App.TestServer.getFactorys().remove(5222864529L);
        App.TestServer.getFactorys().remove(7815467220L);
        App.TestServer.getFactorys().remove(5635082623L);
        App.TestServer.getFactorys().remove(7854078040L);
        App.TestServer.getFactorys().remove(5645907125L);
        App.TestServer.getFactorys().remove(6379586904L);
        App.TestServer.getFactorys().remove(7562655298L);
        App.TestServer.getFactorys().remove(6671907719L);
        App.TestServer.getFactorys().remove(7938190671L);
        App.TestServer.getFactorys().remove(7336162357L);
        // unregister table
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_Table1.getName())).getDatabaseName(), _Table1);
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_Table2.getName())).getDatabaseName(), _Table2);
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_Table3.getName())).getDatabaseName(), _Table3);
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_Table5.getName())).getDatabaseName(), _Table5);
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_TableImportant.getName())).getDatabaseName(), _TableImportant);
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tAutokey.getName())).getDatabaseName(), _tAutokey);
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tAutoKeyRandom.getName())).getDatabaseName(), _tAutoKeyRandom);
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tCoverHistory.getName())).getDatabaseName(), _tCoverHistory);
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tflush.getName())).getDatabaseName(), _tflush);
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tKuafu.getName())).getDatabaseName(), _tKuafu);
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tMemorySize.getName())).getDatabaseName(), _tMemorySize);
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tTestSchemas.getName())).getDatabaseName(), _tTestSchemas);
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tWalkPage.getName())).getDatabaseName(), _tWalkPage);
        // unregister servlet
    }
}
