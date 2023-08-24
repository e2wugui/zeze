package Zeze.Hot;

import java.util.function.Function;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Table;

public class HotUpgradeMemoryTable {
	private final Table old;
	private final Table cur;

	public HotUpgradeMemoryTable(Table old, Table cur) {
		this.old = old;
		this.cur = cur;
	}

	public void upgrade(Function<Bean, Bean> retreatFunc) {
		// todo key retreat
		old.walkMemoryAny((k, v) -> {
			var rBean = retreatFunc.apply(v);
			if (rBean != null) {
				cur.__direct_put_cache__(k, rBean);
			} else {
				cur.__direct_put_cache__(k, v); // retreat 失败，直接加入旧的值。
			}
			return true;
		});
	}
}
