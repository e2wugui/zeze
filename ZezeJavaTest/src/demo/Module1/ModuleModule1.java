package demo.Module1;

// ZEZE_FILE_CHUNK {{{ IMPORT GEN
// ZEZE_FILE_CHUNK }}} IMPORT GEN

public class ModuleModule1 extends AbstractModule {
    public void Start(demo.App app) {
    }

    public void Stop(demo.App app) {
    }

    @Override
    protected long ProcessProtocol3(Zeze.Net.Protocol _p) throws Throwable {
        var p = (Protocol3)_p;
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessProtocol4(Zeze.Net.Protocol _p) throws Throwable {
        var p = (Protocol4)_p;
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessRpc1Request(Zeze.Net.Protocol _r) throws Throwable {
        var r = (Rpc1)_r;
        return Zeze.Transaction.Procedure.NotImplement;
    }
    
    public Table1 getTable1() {
		return _Table1;
	}

	public Table2 getTable2() {
		return _Table2;
	}

	public Table3 getTable3() {
		return _Table3;
	}

	public TableImportant getTableImportant() {
		return _TableImportant;
	}

    @Override
    public long ProcessProtocolNoProcedure(Zeze.Net.Protocol _p) {
        var p = (ProtocolNoProcedure)_p;
        return Zeze.Transaction.Procedure.NotImplement;
    }

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
    public static final int ModuleId = 1;

    private Autokey_ _Autokey_ = new Autokey_();
    private Table1 _Table1 = new Table1();
    private Table2 _Table2 = new Table2();
    private Table3 _Table3 = new Table3();
    private TableImportant _TableImportant = new TableImportant();

    public demo.App App;

    public ModuleModule1(demo.App app) {
        App = app;
        // register protocol factory and handles
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new demo.Module1.Protocol3();
            factoryHandle.Handle = (_p) -> ProcessProtocol3(_p);
            App.Server.AddFactoryHandle(7815467220L, factoryHandle);
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new demo.Module1.Protocol4();
            factoryHandle.Handle = (_p) -> ProcessProtocol4(_p);
            App.Server.AddFactoryHandle(5222864529L, factoryHandle);
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new demo.Module1.ProtocolNoProcedure();
            factoryHandle.Handle = (_p) -> ProcessProtocolNoProcedure(_p);
            factoryHandle.Level = Zeze.Transaction.TransactionLevel.None;
            App.Server.AddFactoryHandle(5645907125L, factoryHandle);
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new demo.Module1.Rpc1();
            factoryHandle.Handle = (_p) -> ProcessRpc1Request(_p);
            App.Server.AddFactoryHandle(5635082623L, factoryHandle);
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new demo.Module1.Rpc2();
            App.Server.AddFactoryHandle(7854078040L, factoryHandle);
        }
        // register table
        App.Zeze.AddTable(App.Zeze.getConfig().GetTableConf(_Autokey_.getName()).getDatabaseName(), _Autokey_);
        App.Zeze.AddTable(App.Zeze.getConfig().GetTableConf(_Table1.getName()).getDatabaseName(), _Table1);
        App.Zeze.AddTable(App.Zeze.getConfig().GetTableConf(_Table2.getName()).getDatabaseName(), _Table2);
        App.Zeze.AddTable(App.Zeze.getConfig().GetTableConf(_Table3.getName()).getDatabaseName(), _Table3);
        App.Zeze.AddTable(App.Zeze.getConfig().GetTableConf(_TableImportant.getName()).getDatabaseName(), _TableImportant);
    }

    public void UnRegister() {
        App.Server.getFactorys().remove(7815467220L);
        App.Server.getFactorys().remove(5222864529L);
        App.Server.getFactorys().remove(5645907125L);
        App.Server.getFactorys().remove(5635082623L);
        App.Server.getFactorys().remove(7854078040L);
        App.Zeze.RemoveTable(App.Zeze.getConfig().GetTableConf(_Autokey_.getName()).getDatabaseName(), _Autokey_);
        App.Zeze.RemoveTable(App.Zeze.getConfig().GetTableConf(_Table1.getName()).getDatabaseName(), _Table1);
        App.Zeze.RemoveTable(App.Zeze.getConfig().GetTableConf(_Table2.getName()).getDatabaseName(), _Table2);
        App.Zeze.RemoveTable(App.Zeze.getConfig().GetTableConf(_Table3.getName()).getDatabaseName(), _Table3);
        App.Zeze.RemoveTable(App.Zeze.getConfig().GetTableConf(_TableImportant.getName()).getDatabaseName(), _TableImportant);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE

}
