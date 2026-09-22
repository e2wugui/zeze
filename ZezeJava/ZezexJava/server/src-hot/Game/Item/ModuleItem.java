package Game.Item;

import Game.*;
import Zeze.Hot.HotService;

public final class ModuleItem extends AbstractModule implements IModuleItem {
	public void Start(App app) {
	}

	public void Stop(App app) {
	}

	@Override
	public void start() throws Exception {
		Start(App);
	}

	@Override
	public void stop() throws Exception {
		Stop(App);
	}

	@Override
	public void upgrade(HotService old) throws Exception {

	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleItem(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
