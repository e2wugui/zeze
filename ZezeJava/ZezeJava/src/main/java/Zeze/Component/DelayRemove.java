package Zeze.Component;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Beans.DelayRemove.BTableKey;
import Zeze.Collections.Queue;
import Zeze.Net.Binary;
import Zeze.Transaction.TableX;

public class DelayRemove extends AbstractDelayRemove {
	public static <K extends Comparable> void remove(TableX table, K key) {
		var zz = table.getZeze();
		var serverId = zz.getConfig().getServerId();
		delays.computeIfAbsent(serverId, (_key_) -> new DelayRemove(zz))._remove(table, key);
	}

	/**
	 * 每个ServerId分配一个独立的GC队列。Server之间不会争抢。如果一个Server一直没有起来，那么它的GC就一直不会执行。
	 */
	private final static ConcurrentHashMap<Integer, DelayRemove> delays = new ConcurrentHashMap<>();

	private Zeze.Collections.Queue<BTableKey> queue;

	private DelayRemove(Zeze.Application zz) {
		var serverId = zz.getConfig().getServerId();
		queue = zz.getQueueModule().open("__GCTableQueue#" + serverId, BTableKey.class);

		// TODO start timer to gc. work on queue.pollNode? peekNode? poll? peek?
	}

	private <K extends Comparable> void _remove(TableX table, K key) {
		var value = new BTableKey();
		value.setTableName(table.getName());
		value.setEncodedKey(new Binary(table.EncodeKey(key)));
		queue.add(value);
	}
}
