package Zeze.Hot;

import java.util.ArrayList;
import Zeze.Transaction.Bean;
import Zeze.Util.Func1;

/**
 * 用于zeze内部，当模块热更的时候升级内部状态。
 */
public interface HotUpgrade {
	void upgrade(ArrayList<HotModule> removes, ArrayList<HotModule> currents, Func1<Bean, Bean> retreatFunc);
}
