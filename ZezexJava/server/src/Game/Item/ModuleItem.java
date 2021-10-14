package Game.Item;

import Game.*;

// auto-generated


public final class ModuleItem extends AbstractModule {
	public void Start(App app) {
	}

	public void Stop(App app) {
	}



	public static final int ModuleId = 3;


	private App App;
	public App getApp() {
		return App;
	}

	public ModuleItem(App app) {
		App = app;
		// register protocol factory and handles
		// register table
	}

	@Override
	public void UnRegister() {
	}

}
