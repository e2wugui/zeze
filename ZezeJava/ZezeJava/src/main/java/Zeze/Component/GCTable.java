package Zeze.Component;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Beans.GCTable.BTableKey;
import Zeze.Collections.Queue;
import Zeze.Net.Binary;
import Zeze.Transaction.TableX;

public class GCTable extends AbstractGCTable {
	public static <K extends Comparable> void gc(TableX table, K key) {
		var zz = table.getZeze();
		var serverId = zz.getConfig().getServerId();
		gcTables.computeIfAbsent(serverId, (_key_) -> new GCTable(zz)).gcTable(table, key);
	}

	/**
	 * 每个ServerId分配一个独立的GC队列。Server之间不会争抢。如果一个Server一直没有起来，那么它的GC就一直不会执行。
	 */
	private final static ConcurrentHashMap<Integer, GCTable> gcTables = new ConcurrentHashMap<>();

	private Zeze.Collections.Queue<BTableKey> queue;

	private GCTable(Zeze.Application zz) {
		var serverId = zz.getConfig().getServerId();
		queue = new Queue<>("__GCTableQueue#" + serverId, BTableKey.class);

		// TODO start timer to gc. work on queue.pollNode? peekNode? poll? peek?
	}

	<K extends Comparable> void gcTable(TableX table, K key) {
		var value = new BTableKey();
		value.setTableName(table.getName());
		value.setEncodedKey(new Binary(table.EncodeKey(key)));
		queue.add(value);
	}
}
