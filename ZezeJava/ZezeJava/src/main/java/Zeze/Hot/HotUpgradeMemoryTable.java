package Zeze.Hot;

import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Transaction.Table;
import Zeze.Util.OutObject;

/**
 * 内存表热更迁移：把旧表数据逐行重解码进新表并禁用旧表，首键不兼容则整体中止。
 */
public class HotUpgradeMemoryTable {
	private final Table old;
	private final Table cur;

	public HotUpgradeMemoryTable(Table old, Table cur) {
		this.old = old;
		this.cur = cur;
	}

	public void upgrade() throws Exception {
		var first = new OutObject<>(true);
		var incompatible = new OutObject<Exception>();
		old.walkMemoryAny((k, v) -> {
			// 检查新表能接受旧表的key类型。只检查一次。
			if (first.value) {
				first.value = false;
				try {
					cur.encodeKey(k);
				} catch (Exception ex) {
					incompatible.value = ex;
					return false; // 首键即中止：cur内零迁移数据，无半迁移
				}
			}
			// key value retreat
			var bbKey = old.encodeKey(k);
			var newKey = cur.decodeKey(bbKey);
			var bbValue = ByteBuffer.Allocate();
			v.encode(bbValue);
			var newValue = cur.newValue();
			newValue.decode(bbValue);
			cur.__direct_put_cache__(newKey, newValue, GlobalCacheManagerConst.StateModify);
			return true;
		});
		if (incompatible.value != null) {
			// fail-fast回滚：首键不兼容即整体中止且保留旧表——静默容忍+仍禁用旧表是半迁移
			// 假成功（数据留在已禁用表上不可达）。本调用处于热更"不能出错阶段"，异常上抛
			// 按该阶段契约停止程序，外部拉起修正jar后重试。
			throw new IllegalStateException("HotUpgradeMemoryTable: new table rejects old key type, keep old table enabled", incompatible.value);
		}
		old.disable();
	}
}
