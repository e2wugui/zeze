package Zeze.Collections;

import java.util.Objects;

import Zeze.Builtin.Collections.Queue.BQueueNode;
import Zeze.Builtin.Collections.Queue.BQueueNodeKey;
import Zeze.Component.Takeover;
import Zeze.Component.TakeoverScope;
import Zeze.Transaction.Bean;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Util.OutLong;
import Zeze.Util.TaskSpec;
import org.jspecify.annotations.Nullable;

/**
 * 并发服务器队列（Concurrent Server Queue）：每个 server 仅操作自有队列，
 * 宕机后由其他 server 通过 {@link TakeoverScope} 租约接管其队列数据。
 */
public class CsQueue<V extends Bean> {
	private final Queue<V> queue;
	private final String name;
	private final Queue.Module module;
	private final CsQueueTakeoverScope takeoverScope;

	/**
	 * 供内部及外部直接构造；应用常规应通过 Queue.Module.open 创建。
	 */
	public CsQueue(Queue.Module module, String name, int serverId, Class<V> valueClass, int nodeSize) {
		this.module = module;
		this.name = name;
		this.queue = module._open(name + "@" + serverId, valueClass, nodeSize);

		// 注册接管作用域：claim/晚注册时写入 root.epoch，写路径 checkFence 据此核对归属。
		var takeover = module.zeze.getTakeover();
		takeoverScope = new CsQueueTakeoverScope();
		takeover.addScope(takeoverScope);
	}

	/**
	 * 队列接管作用域：stamp 写入 root.loadSerialNo=epoch，transferAll 搬运故障节点链表并置墓碑 stamp=0。
	 */
	private final class CsQueueTakeoverScope implements TakeoverScope {
		@Override
		public String name() {
			return "CsQueue(" + getInnerName() + ")";
		}

		@Override
		public void stamp(long epoch) {
			queue.getOrAddRoot().setLoadSerialNo(epoch);
		}

		@Override
		public long transferAll(int deadServerId, long deadEpoch) {
			var srcName = name + "@" + deadServerId;
			var src = Queue.compatible(module._tQueues.get(srcName), srcName);
			if (null == src || src.getHeadNodeKey().getNodeId() == 0 || src.getTailNodeKey().getNodeId() == 0)
				return 0; // 队列不存在、为空或已搬空（幂等出口）

			// 外层已同事务复验租约过期，此处不核对 stamp==deadEpoch，避免 claim 后 stamp 前崩溃导致数据遗留。

			// splice 单向链表，新接管的数据拼到开头。
			var dstName = name + "@" + module.zeze.getConfig().getServerId();
			var dstRoot = Objects.requireNonNull(Queue.compatible(module._tQueues.getOrAdd(dstName), dstName));
			var srcTailNodeKey = src.getTailNodeKey();
			var srcTail = Queue.compatible(srcTailNodeKey, module._tQueueNodes.get(srcTailNodeKey));
			if (null == srcTail)
				throw new IllegalStateException("maybe operate before entry created.");

			srcTail.setNextNodeKey(dstRoot.getHeadNodeKey());
			if (dstRoot.getHeadNodeKey().getNodeId() == 0) // 目标为空时同步更新尾指针，防止后续 add 形成孤岛
				dstRoot.setTailNodeKey(srcTailNodeKey);
			dstRoot.setHeadNodeKey(src.getHeadNodeKey());
			var count = src.getCount();
			dstRoot.setCount(dstRoot.getCount() + count);
			// clear src
			var nullKey = new BQueueNodeKey();
			src.setHeadNodeKey(nullKey);
			src.setTailNodeKey(nullKey);
			// 同步清零废弃的 long 指针，防止重启时 compatible 复活旧头尾。
			src.setHeadNodeId(0);
			src.setTailNodeId(0);
			src.setCount(0);
			src.setLoadSerialNo(0); // 置墓碑 stamp=0，供原节点复活重新 scoped 后认领
			return count;
		}
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

		TaskSpec.ofProcedure(module.zeze.newProcedure(() -> {
			// 接管别的服务器的队列时。
			var srcName = name + "@" + serverId;
			var src = Queue.compatible(module._tQueues.get(srcName), srcName);
			if (null == src || src.getHeadNodeKey().getNodeId() == 0 || src.getTailNodeKey().getNodeId() == 0)
				return 0L; // nothing need to do.

			if (src.getLoadSerialNo() != loadSerialNo)
				return 0L; // 需要接管的机器已经活过来了。

			// prepare splice
			var dstName = name + "@" + module.zeze.getConfig().getServerId();
			var dstRoot = Objects.requireNonNull(Queue.compatible(module._tQueues.getOrAdd(dstName), dstName));
			var srcTailNodeKey = src.getTailNodeKey();
			var srcTail = Queue.compatible(srcTailNodeKey, module._tQueueNodes.get(srcTailNodeKey));

			if (null == srcTail)
				throw new IllegalStateException("maybe operate before entry created.");

			// 这是新接管过来的nodeKey范围，如果需要对新接管数据进一步事务外处理，使用out送出事务外。

			// splice 单向链表，新接管的数据拼到开头。
			srcTail.setNextNodeKey(dstRoot.getHeadNodeKey());
			if (dstRoot.getHeadNodeKey().getNodeId() == 0) // dst是空队列：接管后链尾就是src的尾，否则后续add产生不可达的孤岛节点。
				dstRoot.setTailNodeKey(srcTailNodeKey);
			dstRoot.setHeadNodeKey(src.getHeadNodeKey());
			dstRoot.setCount(dstRoot.getCount() + src.getCount()); // 接管过来的值计入dst。
			// clear src
			var nullKey = new BQueueNodeKey();
			src.setHeadNodeKey(nullKey);
			src.setTailNodeKey(nullKey);
			// 清空时同步清零废弃long字段：空名key+陈旧id并存会让死者重启时compatible复活头指针。
			src.setHeadNodeId(0);
			src.setTailNodeId(0);
			src.setCount(0);
			return 0L;
		}, "CsQueue.splice")).call();
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

	// 写路径 fence：未完成 scope 登记则拒绝写；stamp==0 的无主/墓碑行在 mode=on 时认领为 myEpoch，否则校验归属一致。
	private void checkFence() {
		var takeover = module.zeze.getTakeoverOrNull();
		if (takeover == null)
			return;
		takeover.requireScoped(takeoverScope);
		var root = queue.getOrAddRoot();
		var stamp = root.getLoadSerialNo();
		// 仅 mode=on 时写 stamp，确保 dryrun 不修改数据行、off 不产生冗余写。
		if (stamp == 0 && Takeover.ModeOn.equals(takeover.getMode())) {
			stamp = takeover.getMyEpoch();
			root.setLoadSerialNo(stamp);
		}
		takeover.checkFence(takeoverScope, stamp);
	}

	public @Nullable BQueueNode pollNode() {
		checkFence();
		return queue.pollNode();
	}

	public void clear() {
		checkFence();
		queue.clear();
	}

	public @Nullable BQueueNode peekNode() {
		return queue.peekNode();
	}

	public @Nullable V poll() {
		checkFence();
		return queue.poll();
	}

	public @Nullable V peek() {
		return queue.peek();
	}

	public long size() {
		return queue.size();
	}

	public void add(V value) {
		checkFence();
		queue.add(value);
	}

	public void push(V value) {
		checkFence();
		queue.push(value);
	}

	public @Nullable V pop() {
		checkFence();
		return queue.pop();
	}

	public long walk(TableWalkHandle<BQueueNodeKey, V> func) throws Exception {
		return queue.walk(func);
	}
}
