package Game.Rank;

import Game.*;

// auto-generated


public abstract class AbstractModule implements Zeze.IModule {
	@Override
	public String getFullName() {
		return "Game.Rank";
	}
	@Override
	public String getName() {
		return "Rank";
	}
	@Override
	public int getId() {
		return 9;
	}

	public abstract int ProcessCGetRankList(CGetRankList protocol);

}