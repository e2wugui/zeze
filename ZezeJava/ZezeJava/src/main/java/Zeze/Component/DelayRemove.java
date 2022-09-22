package Zeze.Component;

import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.DelayRemove.BTableKey;
import Zeze.Net.Binary;
import Zeze.Transaction.TableX;
import Zeze.Util.Random;
import Zeze.Util.Task;

public class DelayRemove extends AbstractDelayRemove {
	/**
	 * 每个ServerId分配一个独立的GC队列。Server之间不会争抢。如果一个Server一直没有起来，那么它的GC就一直不会执行。
	 */
	private static final ConcurrentHashMap<Integer, DelayRemove> delays = new ConcurrentHashMap<>();

	public static <K extends Comparable<K>> void remove(TableX<K, ?> table, K key) {
		var zz = table.getZeze();
		var serverId = zz.getConfig().getServerId();
		var delay = delays.computeIfAbsent(serverId, (_key_) -> new DelayRemove(zz));
		var value = new BTableKey();
		value.setTableName(table.getName());
		value.setEncodedKey(new Binary(table.encodeKey(key)));
		value.setEnqueueTime(System.currentTimeMillis());
		delay.queue.add(value);
	}

	private final Zeze.Collections.Queue<BTableKey> queue;
	private Zeze.Application zeze;

	private DelayRemove(Zeze.Application zz) {
		this.zeze = zz;

		var serverId = zz.getConfig().getServerId();
		queue = zz.getQueueModule().open("__GCTableQueue#" + serverId, BTableKey.class);

		// start timer to gc. work on queue.pollNode? peekNode? poll? peek?
		// 根据配置的Timer的时间范围，按分钟精度随机出每天的开始时间，最后计算延迟，然后按24小时间隔执行。
		var firstTime = Calendar.getInstance();
		firstTime.set(Calendar.HOUR_OF_DAY, zz.getConfig().getDelayRemoveHourStart());
		firstTime.set(Calendar.MINUTE, 0);
		firstTime.set(Calendar.SECOND, 0);
		firstTime.set(Calendar.MILLISECOND, 0);

		// rand to end
		var minutes = 60 * (zz.getConfig().getDelayRemoveHourEnd() - zz.getConfig().getDelayRemoveHourStart());
		if (minutes <= 0)
			minutes = 60;
		minutes = Random.getInstance().nextInt(minutes);
		firstTime.add(Calendar.MINUTE, minutes);

		if (firstTime.before(Calendar.getInstance())) // 如果第一次的时间比当前时间早，推到明天。
			firstTime.add(Calendar.DAY_OF_MONTH, 1); // tomorrow!

		var delay = firstTime.getTime().getTime() - System.currentTimeMillis();
		var period = 24 * 3600 * 1000; // 24 hours
		Task.scheduleUnsafe(delay, period, () -> onTimer(serverId));
	}

	private void onTimer(int serverId) {
		// delayRemove可能需要删除很多记录，不嵌入Timer事务，启动新的线程执行新的事务。
		// 这里仅利用Timer的触发。
		// 每个节点的记录删除一个事务执行。
		Task.run(zeze.newProcedure(() -> runDelayRemove(serverId), "delayRemoveProcedure"));
	}

	private long runDelayRemove(int serverId) {
		// 已经在事务中了。
		var days = zeze.getConfig().getDelayRemoveDays();
		if (days < 7)
			days = 7;
		var diffMills = days * 24 * 3600 * 1000;

		var maxTime = 0L; // 放到外面可以处理下面的node.getValues().isEmpty()的情况。
		var node = queue.pollNode();
		for (var value : node.getValues()) {
			var tableKey = (BTableKey)value.getValue().getBean();
			// queue是按时间顺序的，记住最后一条即可，这样写能适应不按顺序的。
			maxTime = Math.max(maxTime, tableKey.getEnqueueTime());
			var table = zeze.getTableSlow(tableKey.getTableName());
			if (null != table)
				table.remove(tableKey.getEncodedKey());
		}
		if (diffMills < System.currentTimeMillis() - maxTime)
			onTimer(serverId); // 都是最老的，再次尝试删除。
		return 0;
	}
}
