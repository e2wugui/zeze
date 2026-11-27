package Zeze.Collections;

import java.util.Objects;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Collections.Queue.BQueue;
import Zeze.Builtin.Collections.Queue.BQueueNode;
import Zeze.Builtin.Collections.Queue.BQueueNodeKey;
import Zeze.Builtin.Collections.Queue.BQueueNodeValue;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Transaction.Transaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 持久化 FIFO 队列：节点分片存储，支持分批读取与断链诊断。
 */
public class Queue<V extends Bean> {
	private static final BeanFactory beanFactory = new BeanFactory();
	private static final Logger logger = LogManager.getLogger(Queue.class);

	public static long getSpecialTypeIdFromBean(Serializable bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public static class Module extends AbstractQueue {
		private final ConcurrentHashMap<String, Queue<?>> queues = new ConcurrentHashMap<>();
		private final ConcurrentHashMap<String, CsQueue<?>> csQueues = new ConcurrentHashMap<>();
		public final Zeze.Application zeze;

		public Module(Zeze.Application zeze) {
			this.zeze = zeze;
			RegisterZezeTables(zeze);
		}

		@Override
		public void UnRegister() {
			UnRegisterZezeTables(zeze);
		}

		public <T extends Bean> Queue<T> open(String name, Class<T> valueClass) {
			return open(name, valueClass, 30);
		}

		public <T extends Bean> Queue<T> open(String name, Class<T> valueClass, int nodeSize) {
			if (name.contains("@"))
				throw new IllegalArgumentException("name contains '@', that is reserved.");
			return _open(name, valueClass, nodeSize);
		}

		@SuppressWarnings("unchecked")
		<T extends Bean> Queue<T> _open(String name, Class<T> valueClass, int nodeSize) {
			if (name.isEmpty())
				throw new IllegalArgumentException("name is empty.");
			if (nodeSize < 1)
				throw new IllegalArgumentException("nodeSize < 1");
			return (Queue<T>)queues.computeIfAbsent(name, key -> new Queue<>(this, key, valueClass, nodeSize));
		}

		// 可以指定serverId，用于测试，但应用确实需要访问指定serverId的CsQueue，也可使用，但要小心。
		@SuppressWarnings("unchecked")
		public <T extends Bean> CsQueue<T> openCsQueue(String name, Class<T> valueClass, int nodeSize) {
			if (name.contains("@"))
				throw new IllegalArgumentException("name contains '@', that is reserved.");
			if (name.isEmpty())
				throw new IllegalArgumentException("name is empty.");
			if (nodeSize < 1)
				throw new IllegalArgumentException("nodeSize < 1");
			// CsQueue 构造时会在 queues 中打开 name@serverId。独立缓存也允许普通队列使用同一 name。
			return (CsQueue<T>)csQueues.computeIfAbsent(name,
					key -> new CsQueue<>(this, key, zeze.getConfig().getServerId(), valueClass, nodeSize));
		}

		public <T extends Bean> CsQueue<T> openCsQueue(String name, Class<T> valueClass) {
			return openCsQueue(name, valueClass, 30);
		}
	}

	private final Module module;
	private final String name;
	private final int nodeSize;

	private Queue(Module module, String name, Class<V> valueClass, int nodeSize) {
		this.module = module;
		this.name = name;
		this.nodeSize = nodeSize;
		beanFactory.register(valueClass);
	}

	public String getName() {
		return name;
	}

	private @Nullable BQueue compatible(@Nullable BQueue root) {
		return compatible(root, name);
	}

	static @Nullable BQueue compatible(@Nullable BQueue root, String name) {
		if (null == root)
			return null;

		if (root.getHeadNodeKey().getName().isEmpty()) {
			var headNodeId = root.getHeadNodeId();
			var tailNodeId = root.getTailNodeId();
			root.setHeadNodeKey(new BQueueNodeKey(name, headNodeId));
			root.setTailNodeKey(new BQueueNodeKey(name, tailNodeId));
			// 迁移后清零废弃的 long 指针，防止清空置空名键时再次复活旧指针。
			root.setHeadNodeId(0);
			root.setTailNodeId(0);
		}
		return root;
	}

	static @Nullable BQueue compatibleDirty(@Nullable BQueue root, String name) {
		if (null == root)
			return null;

		if (root.getHeadNodeKey().getName().isEmpty()) {
			root = root.copy();
			root.setHeadNodeKey(new BQueueNodeKey(name, root.getHeadNodeId()));
			root.setTailNodeKey(new BQueueNodeKey(name, root.getTailNodeId()));
		}
		return root;
	}

	static @Nullable BQueueNode compatible(BQueueNodeKey key, @Nullable BQueueNode node) {
		if (node == null)
			return null;

		if (node.getNextNodeKey().getName().isEmpty()) {
			node.setNextNodeKey(new BQueueNodeKey(key.getName(), node.getNextNodeId()));
		}
		return node;
	}

	static @Nullable BQueueNode compatibleDirty(BQueueNodeKey key, @Nullable BQueueNode node) {
		if (node == null)
			return null;

		if (node.getNextNodeKey().getName().isEmpty()) {
			node = node.copy(); // 只读又需要修改时，复制一份。
			node.setNextNodeKey(new BQueueNodeKey(key.getName(), node.getNextNodeId()));
		}
		return node;
	}

	private @Nullable BQueue getRoot() {
		return compatible(module._tQueues.get(name));
	}

	BQueue getOrAddRoot() {
		return Objects.requireNonNull(compatible(module._tQueues.getOrAdd(name)));
	}

	private @Nullable BQueueNode getNode(BQueueNodeKey key) {
		return compatible(key, module._tQueueNodes.get(key));
	}

	public boolean isEmpty() {
		var root = getRoot();
		return root == null || root.getHeadNodeKey().getNodeId() == 0;
	}

	// 仅记 error 不抛异常：乐观并发重试交错下的瞬时头行缺失会在提交时因读集冲突回滚。
	private void logBrokenHeadChain(String where, BQueue root, BQueueNodeKey headKey) {
		logger.error("queue {}: broken chain, name={}, count={}, headKey={}",
				where, name, root.getCount(), headKey);
	}

	/**
	 * 删除并返回整个头节点
	 *
	 * @return 头节点，null if empty
	 */
	public @Nullable BQueueNode pollNode() {
		var root = getRoot();
		if (root == null)
			return null;

		var headKey = root.getHeadNodeKey();
		if (headKey.getNodeId() == 0)
			return null;

		var head = getNode(headKey);
		if (head == null) {
			logBrokenHeadChain("pollNode", root, headKey);
			return null;
		}

		root.setHeadNodeKey(head.getNextNodeKey());
		root.setCount(root.getCount() - head.getValues().size());
		module._tQueueNodes.remove(headKey);
		// 单节点队列排空时同步清尾键（保留队列名避免 compatible 复活旧指针）。
		if (root.getTailNodeKey().equals(headKey))
			root.setTailNodeKey(new BQueueNodeKey(name, 0));
		return head;
	}

	public void clear() {
		while (pollNode() != null) {
			// do nothing
		}
	}

	/**
	 * 清空节点链并删除队列根行（同事务，回滚时整体还原），事务提交后同步逐出 Module.queues 缓存。
	 * 仅在确认不再使用该队列时调用；后续再次读写会自动重建根行。
	 */
	public void remove() {
		clear();
		if (module._tQueues.get(name) == null)
			return;
		module._tQueues.remove(name);
		// 仅提交成功后按引用匹配逐出缓存，避免回滚误删或删掉并发重建的新包装。
		Transaction.whileCommit(() -> module.queues.remove(name, this));
	}

	/**
	 * @return 头节点，null if empty
	 */
	public @Nullable BQueueNode peekNode() {
		var root = getRoot();
		if (root == null)
			return null;

		var headKey = root.getHeadNodeKey();
		if (headKey.getNodeId() == 0)
			return null;

		var head = getNode(headKey);
		if (head == null) { // 只读变体：诊断后仍按null返回
			logBrokenHeadChain("peekNode", root, headKey);
		}
		return head;
	}

	/**
	 * 删除并返回头节点中的首个值
	 *
	 * @return 头节点的首个值，null if empty
	 */
	public @Nullable V poll() {
		var root = getRoot();
		if (root == null)
			return null;

		var headKey = root.getHeadNodeKey();
		if (headKey.getNodeId() == 0)
			return null;

		var head = getNode(headKey);
		if (head == null) {
			logBrokenHeadChain("poll", root, headKey);
			return null;
		}

		var nodeValues = head.getValues();
		var nodeValue = nodeValues.removeFirst();
		root.setCount(root.getCount() - 1);
		if (nodeValues.isEmpty()) {
			root.setHeadNodeKey(head.getNextNodeKey());
			module._tQueueNodes.remove(headKey);
			// 单节点队列排空时同步清尾键，避免后续 push/add 断链。
			if (root.getTailNodeKey().equals(headKey))
				root.setTailNodeKey(new BQueueNodeKey(name, 0));
		}

		@SuppressWarnings("unchecked")
		var value = (V)nodeValue.getValue().getBean();
		return value;
	}

	/**
	 * @return 头节点的首个值，null if empty
	 */
	public @Nullable V peek() {
		var root = getRoot();
		if (root == null)
			return null;

		var headKey = root.getHeadNodeKey();
		if (headKey.getNodeId() == 0)
			return null;
		var head = getNode(headKey);
		if (head == null) { // 只读变体：诊断后仍按null返回
			logBrokenHeadChain("peek", root, headKey);
			return null;
		}

		@SuppressWarnings("unchecked")
		var value = (V)head.getValues().getFirst().getValue().getBean();
		return value;
	}

	public long size() {
		var root = getRoot();
		return null == root ? 0L : root.getCount();
	}

	/**
	 * 用作queue, 值追加到尾节点的最后, 满则追加一个尾节点。
	 */
	public void add(V value) {
		var root = getOrAddRoot();
		var empty = root.getHeadNodeKey().getNodeId() == 0;
		var tailNodeKey = root.getTailNodeKey();
		// 头键为零时按空队列重新建节点，忽略可能残留的尾键。
		var tail = !empty && tailNodeKey.getNodeId() != 0 ? getNode(tailNodeKey) : null;
		if (tail == null && !empty) {
			// 尾节点缺失时沿头链重定位最后可达节点并修复尾指针，避免新尾节点不可达。
			logger.error("queue add: broken tail, relocate real tail by walking. name={}, count={}, tailKey={}",
					name, root.getCount(), tailNodeKey);
			var curKey = root.getHeadNodeKey();
			tail = getNode(curKey);
			while (tail != null && tail.getNextNodeKey().getNodeId() != 0) {
				var nextKey = tail.getNextNodeKey();
				var next = getNode(nextKey);
				if (next == null) {
					logger.error("queue add: chain broken at {}, use last reachable node as tail. name={}",
							nextKey, name);
					break;
				}
				curKey = nextKey;
				tail = next;
			}
			if (tail != null)
				root.setTailNodeKey(curKey);
		}
		if (tail == null || tail.getValues().size() >= nodeSize) {
			var newNodeId = root.getLastNodeId() + 1;
			root.setLastNodeId(newNodeId);
			var newNodeKey = new BQueueNodeKey(name, newNodeId);
			root.setTailNodeKey(newNodeKey);
			if (empty)
				root.setHeadNodeKey(newNodeKey);
			if (tail != null)
				tail.setNextNodeKey(newNodeKey);
			module._tQueueNodes.insert(newNodeKey, tail = new BQueueNode());
		}
		var nodeValue = new BQueueNodeValue();
		nodeValue.setTimestamp(System.currentTimeMillis());
		nodeValue.getValue().setBean(value);
		tail.getValues().add(nodeValue);
		root.setCount(root.getCount() + 1);
	}

	/**
	 * 用作stack, 值追加到头节点的首位, 满则追加一个头节点
	 */
	public void push(V value) {
		var root = getOrAddRoot();
		var headNodeKey = root.getHeadNodeKey();
		var head = headNodeKey.getNodeId() != 0 ? getNode(headNodeKey) : null;
		if (head == null || head.getValues().size() >= nodeSize) {
			var newNodeId = root.getLastNodeId() + 1;
			root.setLastNodeId(newNodeId);
			var newNodeKey = new BQueueNodeKey(name, newNodeId);
			root.setHeadNodeKey(newNodeKey);
			if (headNodeKey.getNodeId() == 0 || root.getTailNodeKey().getNodeId() == 0)
				root.setTailNodeKey(newNodeKey);
			module._tQueueNodes.insert(newNodeKey, head = new BQueueNode());
			if (headNodeKey.getNodeId() != 0)
				head.setNextNodeKey(headNodeKey);
		}
		var nodeValue = new BQueueNodeValue();
		nodeValue.setTimestamp(System.currentTimeMillis());
		nodeValue.getValue().setBean(value);
		head.getValues().add(0, nodeValue);
		root.setCount(root.getCount() + 1);
	}

	/**
	 * 删除并返回头节点中的首个值
	 *
	 * @return 头节点的首个值，null if empty
	 */
	public @Nullable V pop() {
		return poll();
	}

	// foreach

	/**
	 * 遍历全部元素，func 首参为当前元素所在节点键。
	 * 并发删除导致断链时会从头重启（可能重复回调，count 仅统计最后一轮）；
	 * 重启后仍断在同一节点视为持久断链，抛出 IllegalStateException。
	 */
	@SuppressWarnings("unchecked")
	public long walk(TableWalkHandle<BQueueNodeKey, V> func) throws Exception {
		long count = 0L;
		BQueueNodeKey missingKey = null; // 记录上次断链位置，用于识别持久断链
		while (true) {
			var root = compatibleDirty(module._tQueues.selectDirty(name), name);
			if (null == root)
				return func.endWalk(count); // error break
			var nodeKey = root.getHeadNodeKey();
			count = 0L;
			while (nodeKey.getNodeId() != 0) {
				var node = compatibleDirty(nodeKey, module._tQueueNodes.selectDirty(nodeKey));
				if (null == node) {
					if (nodeKey.equals(missingKey))
						throw new IllegalStateException("Queue.walk broken chain at " + nodeKey);
					missingKey = nodeKey;
					break; // concurrent node remove, restart walk.
				}
				for (var value : node.getValues()) {
					++count;
					if (!func.handle(nodeKey, (V)value.getValue().getBean()))
						return func.endWalk(count); // user break
				}
				nodeKey = node.getNextNodeKey();
			}
			if (nodeKey.getNodeId() == 0)
				return func.endWalk(count); // tail
			// concurrent node remove, restart walk.
		}
	}
}
