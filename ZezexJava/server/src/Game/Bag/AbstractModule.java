package Game.Bag;

import Game.*;

// auto-generated


public abstract class AbstractModule implements Zeze.IModule {
	@Override
	public String getFullName() {
		return "Game.Bag";
	}
	@Override
	public String getName() {
		return "Bag";
	}
	@Override
	public int getId() {
		return 2;
	}

	public static final int ResultCodeFromInvalid = 1;
	public static final int ResultCodeToInvalid = 2;
	public static final int ResultCodeFromNotExsit = 3;
	public static final int ResultCodeTrySplitButTargetExsitDifferenceItem = 4;

	public abstract int ProcessCUse(CUse protocol);

	public abstract int ProcessDestroyRequest(Destroy rpc);

	public abstract int ProcessGetBagRequest(GetBag rpc);

	public abstract int ProcessMoveRequest(Move rpc);

	public abstract int ProcessSortRequest(Sort rpc);

}