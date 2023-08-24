package Zeze.Hot;

import java.util.function.Function;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Table;
import Zeze.Util.OutObject;

public class HotUpgradeMemoryTable {
	private final Table old;
	private final Table cur;

	public HotUpgradeMemoryTable(Table old, Table cur) {
		this.old = old;
		this.cur = cur;
	}

	public void upgrade(Function<Bean, Bean> retreatFunc) {
		var first = new OutObject<>(true);
		try {
			old.walkMemoryAny((k, v) -> {
				// 检查新表能接受旧表的key类型。只检查一次。
				if (first.value) {
					first.value = false;
					try {
						cur.encodeKey(k);
					} catch (Exception ex) {
						return false;
					}
				}
				// key value retreat
				var bbKey = old.encodeKey(k);
				var newKey = cur.decodeKeyToObject(bbKey);
				var rBean = retreatFunc.apply(v);
				if (rBean != null) {
					cur.__direct_put_cache__(newKey, rBean);
				} else {
					cur.__direct_put_cache__(newKey, v); // retreat 失败，直接加入旧的值。
				}
				return true;
			});
		} finally {
			old.disable();
		}
	}
}
