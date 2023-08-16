package Zeze.Hot;

import java.util.ArrayList;
import java.util.function.Function;
import Zeze.Transaction.Bean;

/**
 * 用于zeze内部，当模块热更的时候升级内部状态。
 */
public interface HotUpgrade {
	void upgrade(ArrayList<HotModule> removes, ArrayList<HotModule> currents, Function<Bean, Bean> retreatFunc);

	// 检查最近是否有跟自己有关的刚停止的模块。读取一次以后重置。
	boolean hasFreshStopModuleOnce();
}
