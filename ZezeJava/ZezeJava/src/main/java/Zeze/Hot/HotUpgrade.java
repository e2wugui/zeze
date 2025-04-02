package Zeze.Hot;

import java.util.function.Function;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;

/**
 * 用于zeze内部，当模块热更的时候升级内部状态。
 */
public interface HotUpgrade {
	void upgrade(@NotNull Function<Bean, Bean> retreatFunc) throws Exception;

	// 检查最近是否有跟自己有关的刚停止的模块。读取一次以后重置成false。
	boolean hasFreshStopModuleLocalOnce();
}
