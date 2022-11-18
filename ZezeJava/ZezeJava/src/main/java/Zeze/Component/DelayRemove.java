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
	private final ConcurrentHashMap<Integer, DelayRemove> delays = new ConcurrentHashMap<>();

	public <K extends Comparable<K>> void remove(TableX<K, ?> table, K key) {
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
	private final Zeze.Application zeze;

	public DelayRemove(Zeze.Application zz) {
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
		Task.scheduleUnsafe(delay, period, this::onTimer);
	}

	private void onTimer() throws Throwable {
		// delayRemove可能需要删除很多记录，不能在一个事务内完成全部删除。
		// 这里按每个节点的记录的删除在一个事务中执行，节点间用不同的事务。
		var days = zeze.getConfig().getDelayRemoveDays();
		if (days < 7)
			days = 7; // xxx 至少保留7天。
		var diffMills = days * 24 * 3600 * 1000;
		var removing = new Zeze.Util.OutObject<>(true);
		while (removing.value) {
			zeze.newProcedure(() -> {
				var node = queue.pollNode();
				if (node == null) {
					removing.value = false;
					return 0;
				}

				// 检查节点的第一个（最老的）项是否需要删除。
				// 如果不需要，那么整个节点都不会删除，并且中断循环。
				// 如果需要，那么整个节点都删除，即使中间有一些没有达到过期。
				// 这是个不精确的删除过期的方法。
				if (!node.getValues().isEmpty()) {
					var first = (BTableKey)node.getValues().get(0).getValue().getBean();
					if (diffMills < System.currentTimeMillis() - first.getEnqueueTime()) {
						removing.value = false;
						return 0;
					}
				}

				// node.getValues().isEmpty，这一项将保持0，循环后设置removing.value将基本是true。
				// 即，空节点总是尝试继续删除。
				long maxTime = 0;
				for (var value : node.getValues()) {
					var tableKey = (BTableKey)value.getValue().getBean();
					// queue是按时间顺序的，记住最后一条即可。
					maxTime = tableKey.getEnqueueTime();
					var table = zeze.getTable(tableKey.getTableName());
					if (null != table)
						table.remove(tableKey.getEncodedKey());
				}
				removing.value = diffMills < System.currentTimeMillis() - maxTime;
				return 0;
			}, "delayRemoveProcedure").call();
		}
	}
}
