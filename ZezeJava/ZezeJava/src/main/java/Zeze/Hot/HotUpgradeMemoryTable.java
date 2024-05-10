package Zeze.Hot;

import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Transaction.Table;
import Zeze.Util.OutObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HotUpgradeMemoryTable {
	private static final Logger logger = LogManager.getLogger(HotUpgradeMemoryTable.class);
	private final Table old;
	private final Table cur;

	public HotUpgradeMemoryTable(Table old, Table cur) {
		this.old = old;
		this.cur = cur;
	}

	public void upgrade() throws Exception {
		var first = new OutObject<>(true);
		try {
			old.walkMemoryAny((k, v) -> {
				// 检查新表能接受旧表的key类型。只检查一次。
				if (first.value) {
					first.value = false;
					try {
						cur.encodeKey(k);
					} catch (Exception ex) {
						logger.error("", ex);
						return false;
					}
				}
				// key value retreat
				var bbKey = old.encodeKey(k);
				var newKey = cur.decodeKey(bbKey);
				var bbValue = ByteBuffer.Allocate();
				v.encode(bbValue);
				var newValue = cur.newValue();
				newValue.decode(bbValue);
				//logger.info("retreat: " + newKey + " " + newValue);
				cur.__direct_put_cache__(newKey, newValue, GlobalCacheManagerConst.StateModify);
				return true;
			});
		} finally {
			old.disable();
		}
	}
}
