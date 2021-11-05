package Game.Map;

import Zeze.Transaction.*;
import Game.*;
import Zeze.Net.Protocol;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class ModuleMap extends AbstractModule {
	public void Start(App app) {
	}

	public void Stop(App app) {
	}

	@Override
	public long ProcessCEnterWorld(Protocol _protocol) {
        var protocol = (CEnterWorld)_protocol;
		Game.Login.Session session = Game.Login.Session.Get(protocol);
		if (session.getRoleId() == null) {
			return Procedure.LogicError;
		}

		// TODO map
		return Procedure.NotImplement;
	}

	@Override
	public long ProcessCEnterWorldDone(Protocol _protocol) {
        var protocol = (CEnterWorldDone)_protocol;
		// TODO map
		return Procedure.NotImplement;
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
    public static final int ModuleId = 8;


    public Game.App App;

    public ModuleMap(Game.App app) {
        App = app;
        // register protocol factory and handles
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Game.Map.CEnterWorld();
            factoryHandle.Handle = (_p) -> ProcessCEnterWorld(_p);
            App.Server.AddFactoryHandle(35514358966L, factoryHandle);
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Game.Map.CEnterWorldDone();
            factoryHandle.Handle = (_p) -> ProcessCEnterWorldDone(_p);
            App.Server.AddFactoryHandle(35705348604L, factoryHandle);
        }
        // register table
    }

    public void UnRegister() {
        App.Server.getFactorys().remove(35514358966L);
        App.Server.getFactorys().remove(35705348604L);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
