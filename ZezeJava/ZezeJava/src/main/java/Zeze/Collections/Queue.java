package Zeze.Collections;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Collections.Queue.BQueue;
import Zeze.Builtin.Collections.Queue.BQueueNode;
import Zeze.Builtin.Collections.Queue.BQueueNodeKey;
import Zeze.Builtin.Collections.Queue.BQueueNodeValue;
import Zeze.Hot.HotBeanFactory;
import Zeze.Hot.HotManager;
import Zeze.Hot.HotModule;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Util.ConcurrentHashSet;

public class Queue<V extends Bean> implements HotBeanFactory {
	private static final BeanFactory beanFactory = new BeanFactory();

	public static long getSpecialTypeIdFromBean(Serializable bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	private final ConcurrentHashSet<HotModule> hotModulesHaveDynamic = new ConcurrentHashSet<>();
	private boolean freshStopModuleDynamic = false;

	private void onHotModuleStop(HotModule hot) {
		freshStopModuleDynamic |= hotModulesHaveDynamic.remove(hot) != null;
	}

	private void tryRecordHotModule(Class<?> customClass) {
		var cl = customClass.getClassLoader();
		if (HotManager.isHotModule(cl)) {
			var hotModule = (HotModule)cl;
			hotModule.stopEvents.add(this::onHotModuleStop);
			hotModulesHaveDynamic.add(hotModule);
		}
	}

	@Override
	public void processWithNewClasses(java.util.List<Class<?>> newClasses) {
		for (var cls : newClasses) {
			tryRecordHotModule(cls);
		}
	}

	@Override
	public void clearTableCache() {
		module._tQueueNodes.__ClearTableCacheUnsafe__();
	}

	@Override
	public BeanFactory beanFactory() {
		return beanFactory;
	}

	@Override
	public boolean hasFreshStopModuleDynamicOnce() {
		var tmp = freshStopModuleDynamic;
		freshStopModuleDynamic = false;
		return tmp;
	}

	public static class Module extends AbstractQueue {
		private final ConcurrentHashMap<String, Object> queues = new ConcurrentHashMap<>();
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
			return open(name, valueClass, 100);
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
			return (CsQueue<T>)queues.computeIfAbsent(name,
					key -> new CsQueue<>(this, key, zeze.getConfig().getServerId(), valueClass, nodeSize));
		}

		public <T extends Bean> CsQueue<T> openCsQueue(String name, Class<T> valueClass) {
			return openCsQueue(name, valueClass, 100);
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

		var hotManager = module.zeze.getHotManager();
		if (null != hotManager) {
			hotManager.addHotBeanFactory(this);
			tryRecordHotModule(valueClass);
		}
	}

	public String getName() {
		return name;
	}

	private BQueue compatible(BQueue root) {
		return compatible(root, name);
	}

	static BQueue compatible(BQueue root, String name) {
		if (null == root)
			return null;

		if (root.getHeadNodeKey().getName().isEmpty()) {
			root.setHeadNodeKey(new BQueueNodeKey(name, root.getHeadNodeId()));
			root.setTailNodeKey(new BQueueNodeKey(name, root.getTailNodeId()));
		}
		return root;
	}

	static BQueue compatibleDirty(BQueue root, String name) {
		if (null == root)
			return null;

		if (root.getHeadNodeKey().getName().isEmpty()) {
			root = root.copy();
			root.setHeadNodeKey(new BQueueNodeKey(name, root.getHeadNodeId()));
			root.setTailNodeKey(new BQueueNodeKey(name, root.getTailNodeId()));
		}
		return root;
	}

	static BQueueNode compatible(BQueueNodeKey key, BQueueNode node) {
		if (node == null)
			return null;

		if (node.getNextNodeKey().getName().isEmpty()) {
			node.setNextNodeKey(new BQueueNodeKey(key.getName(), node.getNextNodeId()));
		}
		return node;
	}

	static BQueueNode compatibleDirty(BQueueNodeKey key, BQueueNode node) {
		if (node == null)
			return null;

		if (node.getNextNodeKey().getName().isEmpty()) {
			node = node.copy(); // 只读又需要修改时，复制一份。
			node.setNextNodeKey(new BQueueNodeKey(key.getName(), node.getNextNodeId()));
		}
		return node;
	}

	private BQueue getRoot() {
		return compatible(module._tQueues.get(name));
	}

	BQueue getOrAddRoot() {
		return compatible(module._tQueues.getOrAdd(name));
	}

	private BQueueNode getNode(BQueueNodeKey key) {
		return compatible(key, module._tQueueNodes.get(key));
	}

	public boolean isEmpty() {
		var root = getRoot();
		return root == null || root.getHeadNodeKey().getNodeId() == 0;
	}

	/**
	 * 删除并返回整个头节点
	 *
	 * @return 头节点，null if empty
	 */
	public BQueueNode pollNode() {
		var root = getRoot();
		if (root == null)
			return null;

		var headKey = root.getHeadNodeKey();
		if (headKey.getNodeId() == 0)
			return null;

		var head = getNode(headKey);
		if (head == null)
			return null;

		root.setHeadNodeKey(head.getNextNodeKey());
		root.setCount(root.getCount() - head.getValues().size());
		module._tQueueNodes.remove(headKey);
		return head;
	}

	public void clear() {
		while (pollNode() != null) {
			// do nothing
		}
	}

	/**
	 * @return 头节点，null if empty
	 */
	public BQueueNode peekNode() {
		var root = getRoot();
		if (root == null)
			return null;

		var headKey = root.getHeadNodeKey();
		if (headKey.getNodeId() == 0)
			return null;

		return getNode(headKey);
	}

	/**
	 * 删除并返回头节点中的首个值
	 *
	 * @return 头节点的首个值，null if empty
	 */
	public V poll() {
		var root = getRoot();
		if (root == null)
			return null;

		var headKey = root.getHeadNodeKey();
		if (headKey.getNodeId() == 0)
			return null;

		var head = getNode(headKey);
		if (head == null)
			return null;

		var nodeValues = head.getValues();
		var nodeValue = nodeValues.remove(0);
		root.setCount(root.getCount() - 1);
		if (nodeValues.isEmpty()) {
			root.setHeadNodeKey(head.getNextNodeKey());
			module._tQueueNodes.remove(headKey);
		}

		@SuppressWarnings("unchecked")
		var value = (V)nodeValue.getValue().getBean();
		return value;
	}

	/**
	 * @return 头节点的首个值，null if empty
	 */
	public V peek() {
		var root = getRoot();
		if (root == null)
			return null;

		var headKey = root.getHeadNodeKey();
		if (headKey.getNodeId() == 0)
			return null;
		var head = getNode(headKey);
		if (head == null)
			return null;

		@SuppressWarnings("unchecked")
		var value = (V)head.getValues().get(0).getValue().getBean();
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
		var tailNodeKey = root.getTailNodeKey();
		var tail = tailNodeKey.getNodeId() != 0 ? getNode(tailNodeKey) : null; // 比起直接访问快一些。
		if (tail == null || tail.getValues().size() >= nodeSize) {
			var newNodeId = root.getLastNodeId() + 1;
			root.setLastNodeId(newNodeId);
			var newNodeKey = new BQueueNodeKey(name, newNodeId);
			root.setTailNodeKey(newNodeKey);
			if (root.getHeadNodeKey().getNodeId() == 0)
				root.setHeadNodeKey(newNodeKey);
			if (tail != null)
				tail.setNextNodeKey(newNodeKey);
			module._tQueueNodes.insert(newNodeKey, tail = new BQueueNode());
			root.setCount(root.getCount() + 1);
		}
		var nodeValue = new BQueueNodeValue();
		nodeValue.setTimestamp(System.currentTimeMillis());
		nodeValue.getValue().setBean(value);
		tail.getValues().add(nodeValue);
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
			if (root.getTailNodeKey().getNodeId() == 0)
				root.setTailNodeKey(newNodeKey);
			module._tQueueNodes.insert(newNodeKey, head = new BQueueNode());
			root.setCount(root.getCount() + 1);
			if (headNodeKey.getNodeId() != 0)
				head.setNextNodeKey(headNodeKey);
		}
		var nodeValue = new BQueueNodeValue();
		nodeValue.setTimestamp(System.currentTimeMillis());
		nodeValue.getValue().setBean(value);
		head.getValues().add(0, nodeValue);
	}

	/**
	 * 删除并返回头节点中的首个值
	 *
	 * @return 头节点的首个值，null if empty
	 */
	public V pop() {
		return poll();
	}

	// foreach

	/**
	 * func 第一个参数是当前Value所在的Node.Id。
	 */
	@SuppressWarnings("unchecked")
	public long walk(TableWalkHandle<BQueueNodeKey, V> func) throws Exception {
		long count = 0L;
		while (true) {
			var root = compatibleDirty(module._tQueues.selectDirty(name), name);
			if (null == root)
				return count; // error break
			var nodeKey = root.getHeadNodeKey();
			while (nodeKey.getNodeId() != 0) {
				var node = compatibleDirty(nodeKey, module._tQueueNodes.selectDirty(nodeKey));
				if (null == node)
					break; // concurrent node remove, restart walk.
				for (var value : node.getValues()) {
					++count;
					if (!func.handle(nodeKey, (V)value.getValue().getBean()))
						return count; // user break
				}
				nodeKey = node.getNextNodeKey();
			}
			if (nodeKey.getNodeId() == 0)
				return count; // tail
			// concurrent node remove, restart walk.
		}
	}
}
