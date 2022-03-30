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
    protected long ProcessCEnterWorld(CEnterWorld protocol) throws Throwable {
		Game.Login.Session session = Game.Login.Session.Get(protocol);
		if (session.getRoleId() == null) {
			return Procedure.LogicError;
		}

		// TODO map
		return Procedure.NotImplement;
	}

	@Override
    protected long ProcessCEnterWorldDone(CEnterWorldDone protocol) throws Throwable {
		// TODO map
		return Procedure.NotImplement;
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
    public ModuleMap(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
