package Game.Item;

import Game.*;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class ModuleItem extends AbstractModule {
	public void Start(App app) {
	}

	public void Stop(App app) {
	}


	// ZEZE_FILE_CHUNK {{{ GEN MODULE
    public static final int ModuleId = 3;


    public Game.App App;

    public ModuleItem(Game.App app) {
        App = app;
        // register protocol factory and handles
        // register table
    }

    public void UnRegister() {
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
