package Game.Map;

import Zeze.Transaction.*;
import Game.*;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class ModuleMap extends AbstractModule {
	public void Start(App app) {
	}

	public void Stop(App app) {
	}

	@Override
	public int ProcessCEnterWorld(CEnterWorld protocol) {
		Game.Login.Session session = Game.Login.Session.Get(protocol);
		if (session.getRoleId().equals(null)) {
			return Procedure.LogicError;
		}

		// TODO map
		return Procedure.NotImplement;
	}

	@Override
	public int ProcessCEnterWorldDone(CEnterWorldDone protocol) {
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
            App.Server.AddFactoryHandle(546916, factoryHandle);
       }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle();
            factoryHandle.Factory = () -> new Game.Map.CEnterWorldDone();
            factoryHandle.Handle = (_p) -> ProcessCEnterWorldDone(_p);
            App.Server.AddFactoryHandle(537032, factoryHandle);
       }
        // register table
    }

    public void UnRegister() {
        App.Server.getFactorys().remove(546916);
        App.Server.getFactorys().remove(537032);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
