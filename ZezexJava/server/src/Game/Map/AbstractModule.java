package Game.Map;

import Game.*;

// auto-generated


public abstract class AbstractModule implements Zeze.IModule {
	@Override
	public String getFullName() {
		return "Game.Map";
	}
	@Override
	public String getName() {
		return "Map";
	}
	@Override
	public int getId() {
		return 8;
	}

	public abstract int ProcessCEnterWorld(CEnterWorld protocol);

	public abstract int ProcessCEnterWorldDone(CEnterWorldDone protocol);

}