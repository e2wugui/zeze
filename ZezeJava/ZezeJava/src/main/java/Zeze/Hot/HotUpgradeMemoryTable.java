package Zeze.Hot;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Table;
import Zeze.Util.OutObject;

public class HotUpgradeMemoryTable {
	private final Table old;
	private final Table cur;

	public HotUpgradeMemoryTable(Table old, Table cur) {
		this.old = old;
		this.cur = cur;
	}

	public void upgrade() {
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
				var newKey = cur.decodeKeyObject(bbKey);
				var bbValue = ByteBuffer.Allocate();
				v.encode(bbValue);
				var newValue = cur.newValueBean();
				newValue.decode(bbValue);
				cur.__direct_put_cache__(newKey, newValue);
				return true;
			});
		} finally {
			old.disable();
		}
	}
}
