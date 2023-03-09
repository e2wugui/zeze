package Game.Map;

import Zeze.Arch.ProviderUserSession;
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
    protected long ProcessCEnterWorld(CEnterWorld protocol) throws Exception {
		var session = ProviderUserSession.get(protocol);
		if (session.getRoleId() == null) {
			return Procedure.LogicError;
		}

		// map
		return Procedure.NotImplement;
	}

	@Override
    protected long ProcessCEnterWorldDone(CEnterWorldDone protocol) throws Exception {
		// map
		return Procedure.NotImplement;
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
    public ModuleMap(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
