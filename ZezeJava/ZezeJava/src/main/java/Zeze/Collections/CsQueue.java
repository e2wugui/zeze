package Zeze.Collections;

import Zeze.Builtin.Collections.Queue.BQueueNode;
import Zeze.Builtin.Collections.Queue.BQueueNodeKey;
import Zeze.Component.Takeover;
import Zeze.Component.TakeoverScope;
import Zeze.Transaction.Bean;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Util.OutLong;
import Zeze.Util.TaskSpec;
import org.jetbrains.annotations.Nullable;

/**
 * Concurrent Server Queue.
 * 每个server拥有自己私有的队列，只能操作自己的队列。
 * server宕机的时候，其他server会接管它的队列数据（接管裁决走Takeover租约：see TakeoverScope）。
 * @param <V>
 */
public class CsQueue<V extends Bean> {
	private final Queue<V> queue;
	private final String name;
	private final Queue.Module module;
	private final @Nullable CsQueueTakeoverScope takeoverScope;

	/**
	 * 为了测试公开这个方法，应用应该去使用Queue.Module.open.
	 */
	public CsQueue(Queue.Module module, String name, int serverId, Class<V> valueClass, int nodeSize) {
		this.module = module;
		this.name = name;
		this.queue = module._open(name + "@" + serverId, valueClass, nodeSize);

		// 接管租约注册：claim/晚注册时stamp自己root行的epoch（替代旧offlineRegister的bump serial），
		// 死者数据由takeover.tryTransfer在同一事务内裁决+搬运（附带修复：SM=disable时原ctor NPE）。
		// scope存为成员：写路径checkFence需要它向Takeover对账scoped登记状态。
		var takeover = module.zeze.getTakeover();
		if (takeover != null) {
			takeoverScope = new CsQueueTakeoverScope();
			takeover.addScope(takeoverScope);
		} else
			takeoverScope = null;
	}

	/**
	 * 本队列的接管作用域：scope.name=队列名；数据行=tQueues[name@serverId]。
	 * stamp写root.loadSerialNo=epoch（fence对账值）；transferAll搬运死者整条链并给死者root立墓碑stamp=0。
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
				return 0; // 死者没有这个队列/空队列/已被搬走（成功搬运后头尾必为0，幂等出口）。

			// FND2-C0-1：不核对stamp==deadEpoch。调用点（Takeover.transferScope）已在同一事务内
			// 重验租约过期，复活者被NotExpired拦截；而"stamp≠epoch但链上有数据"只可能是死者
			// claim后、stamp前崩溃（或双跳崩溃留下上一代stamp）——数据仍属于死者，必须搬走。
			// 旧守卫在这里return 0，外层照常立租约墓碑（scanOnce永久跳过墓碑），积压被永久封存。

			// splice 单向链表，新接管的数据拼到开头。
			var dstName = name + "@" + module.zeze.getConfig().getServerId();
			var dstRoot = Queue.compatible(module._tQueues.getOrAdd(dstName), dstName);
			var srcTailNodeKey = src.getTailNodeKey();
			var srcTail = Queue.compatible(srcTailNodeKey, module._tQueueNodes.get(srcTailNodeKey));
			if (null == srcTail)
				throw new IllegalStateException("maybe operate before entry created.");

			srcTail.setNextNodeKey(dstRoot.getHeadNodeKey());
			if (dstRoot.getHeadNodeKey().getNodeId() == 0) // dst是空队列：接管后链尾就是src的尾，否则后续add产生不可达的孤岛节点。
				dstRoot.setTailNodeKey(srcTailNodeKey);
			dstRoot.setHeadNodeKey(src.getHeadNodeKey());
			var count = src.getCount();
			dstRoot.setCount(dstRoot.getCount() + count); // 接管过来的值计入dst。
			// clear src
			var nullKey = new BQueueNodeKey();
			src.setHeadNodeKey(nullKey);
			src.setTailNodeKey(nullKey);
			// 清空时同步清零废弃long字段：空名key+陈旧id并存会让死者重启时compatible复活头指针。
			src.setHeadNodeId(0);
			src.setTailNodeId(0);
			src.setCount(0);
			src.setLoadSerialNo(0); // 死者root清账stamp=0（墓碑）：幂等重入由上面头尾==0短路；复活者醒来scoped后认领继续写。
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

	// 写路径fence：root行本就在事务工作集内，零额外IO。被接管（root.epoch != myEpoch）→致命退出。
	// 未完成stamp登记（stamp瞬态失败等，FND-C1-11）→NotStartException拒绝：不认领数据行、不致命，
	// renew周期补stamp后恢复；root.stamp==0（无主新行/外部清表重建/被接管后的墓碑）仅scoped后
	// 认领（stamp=myEpoch）而非致命：被接管者醒来在空链上复活并继续新写（需求语义）；fence只杀
	// 同serverId双进程/外部篡改（stamp为别人的epoch）。
	private void checkFence() {
		var takeover = module.zeze.getTakeover();
		if (takeover == null || takeoverScope == null)
			return;
		takeover.requireScoped(takeoverScope); // 未登记：拒绝写，且不认领数据行
		var root = queue.getOrAddRoot();
		var stamp = root.getLoadSerialNo();
		// FND2-C0-4：认领写（setLoadSerialNo）仅mode=on——与Takeover.start自己stampScope的
		// 前置一致。dryrun必须守住"纯簿记不动数据行"的灰度契约；off避免0→0冗余写。
		// 不认领时stamp保持0，checkFence对mode!=on恒通过，不影响早退路径。
		if (stamp == 0 && Takeover.ModeOn.equals(takeover.getMode())) {
			stamp = takeover.getMyEpoch();
			root.setLoadSerialNo(stamp);
		}
		takeover.checkFence(takeoverScope, stamp);
	}

	public BQueueNode pollNode() {
		checkFence();
		return queue.pollNode();
	}

	public void clear() {
		checkFence();
		queue.clear();
	}

	public BQueueNode peekNode() {
		return queue.peekNode();
	}

	public V poll() {
		checkFence();
		return queue.poll();
	}

	public V peek() {
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

	public V pop() {
		checkFence();
		return queue.pop();
	}

	public long walk(TableWalkHandle<BQueueNodeKey, V> func) throws Exception {
		return queue.walk(func);
	}
}
