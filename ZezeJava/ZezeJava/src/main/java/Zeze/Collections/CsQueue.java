package Zeze.Collections;

import Zeze.Builtin.Collections.Queue.BQueueNode;
import Zeze.Builtin.Collections.Queue.BQueueNodeKey;
import Zeze.Services.ServiceManager.BOfflineNotify;
import Zeze.Transaction.Bean;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Util.OutLong;
import Zeze.Util.Task;

/**
 * Concurrent Server Queue.
 * 每个server拥有自己私有的队列，只能操作自己的队列。
 * server宕机的时候，其他server会接管它的队列数据。
 * @param <V>
 */
public class CsQueue<V extends Bean> {
	private final Queue<V> queue;
	private final String name;
	private final Queue.Module module;

	/**
	 * 为了测试公开这个方法，应用应该去使用Queue.Module.open.
	 */
	public CsQueue(Queue.Module module, String name, int serverId, Class<V> valueClass, int nodeSize) {
		this.module = module;
		this.name = name;
		this.queue = module._open(name + "@" + serverId, valueClass, nodeSize);

		var out = new OutLong();
		Task.call(module.zeze.newProcedure(() -> {
			var root = queue.getOrAddRoot();
			root.setLastNodeId(root.getLoadSerialNo() + 1);
			out.value = root.getLoadSerialNo();
			return 0;
		}, "increaseLoadSerialNo"));
		var offlineNotify = new BOfflineNotify();
		offlineNotify.serverId = module.zeze.getConfig().getServerId();
		offlineNotify.notifySerialId = out.value;
		offlineNotify.notifyId = "Zeze.Collections.CsQueue.OfflineNotify";
		module.zeze.getServiceManager().offlineRegister(offlineNotify,
				(notify) -> splice(notify.serverId, notify.notifySerialId));
	}

	public long getLoadSerialNo() {
		var out = new OutLong();
		module.zeze.newProcedure(() -> {
			out.value = queue.getOrAddRoot().getLoadSerialNo();
			return 0;
		}, "getLoadSerialNo@" + getName()).call();
		return out.value;
	}

	/**
	 * 为了测试了公开的，调用也是可以的，但要小心。
	 * @param serverId serverId
	 * @param loadSerialNo loadSerialNo
	 */
	public void splice(int serverId, long loadSerialNo) {
		if (serverId == module.zeze.getConfig().getServerId())
			return; // skip self

		Task.call(module.zeze.newProcedure(() -> {
			// 接管别的服务器的队列时。
			var srcName = name + "@" + serverId;
			var src = Queue.compatible(module._tQueues.get(srcName), srcName);
			if (null == src || src.getHeadNodeKey().getNodeId() == 0 || src.getTailNodeKey().getNodeId() == 0)
				return 0L; // nothing need to do.

			if (src.getLoadSerialNo() != loadSerialNo)
				return 0L; // 需要接管的机器已经活过来了。

			// prepare splice
			var dstName = name + "@" + module.zeze.getConfig().getServerId();
			var dstRoot = Queue.compatible(module._tQueues.getOrAdd(dstName), dstName);
			var srcTailNodeKey = src.getTailNodeKey();
			var srcTail = Queue.compatible(srcTailNodeKey, module._tQueueNodes.get(srcTailNodeKey));

			if (null == srcTail)
				throw new IllegalStateException("maybe operate before entry created.");

			// 这是新接管过来的nodeKey范围，如果需要对新接管数据进一步事务外处理，使用out送出事务外。
			//first.value = new BQueueNodeKey(srcName, src.getHeadNodeId());
			//last.value = new BQueueNodeKey(dstName, dstRoot.getHeadNodeId());

			// splice 单向链表，新接管的数据拼到开头。
			srcTail.setNextNodeKey(dstRoot.getHeadNodeKey());
			dstRoot.setHeadNodeKey(src.getHeadNodeKey());
			// clear src
			var nullKey = new BQueueNodeKey();
			src.setHeadNodeKey(nullKey);
			src.setTailNodeKey(nullKey);
			return 0L;
		}, "CsQueue.splice"));
	}

	public String getName() {
		return name;
	}

	public String getInnerName() {
		return queue.getName();
	}

	public boolean isEmpty() {
		return queue.isEmpty();
	}

	public BQueueNode pollNode() {
		return queue.pollNode();
	}

	public void clear() {
		queue.clear();
	}

	public BQueueNode peekNode() {
		return queue.peekNode();
	}

	public V poll() {
		return queue.poll();
	}

	public V peek() {
		return queue.peek();
	}

	public long size() {
		return queue.size();
	}

	public void add(V value) {
		queue.add(value);
	}

	public void push(V value) {
		queue.push(value);
	}

	public V pop() {
		return queue.pop();
	}

	public long walk(TableWalkHandle<BQueueNodeKey, V> func) {
		return queue.walk(func);
	}
}
