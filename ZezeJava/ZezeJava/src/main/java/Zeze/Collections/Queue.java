package Zeze.Collections;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Collections.Queue.BQueueNode;
import Zeze.Builtin.Collections.Queue.BQueueNodeKey;
import Zeze.Builtin.Collections.Queue.BQueueNodeValue;
import Zeze.Hot.HotBeanFactory;
import Zeze.Hot.HotManager;
import Zeze.Hot.HotModule;
import Zeze.Transaction.Bean;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Util.ConcurrentHashSet;

public class Queue<V extends Bean> implements HotBeanFactory {
	private static final BeanFactory beanFactory = new BeanFactory();

	public static long getSpecialTypeIdFromBean(Bean bean) {
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
		private final ConcurrentHashMap<String, Queue<?>> queues = new ConcurrentHashMap<>();
		public final Zeze.Application zeze;

		public Module(Zeze.Application zeze) {
			this.zeze = zeze;
			RegisterZezeTables(zeze);
		}

		@Override
		public void UnRegister() {
			UnRegisterZezeTables(zeze);
		}

		@SuppressWarnings("unchecked")
		public <T extends Bean> Queue<T> open(String name, Class<T> valueClass) {
			return (Queue<T>)queues.computeIfAbsent(name, key -> new Queue<>(this, key, valueClass, 100));
		}

		@SuppressWarnings("unchecked")
		public <T extends Bean> Queue<T> open(String name, Class<T> valueClass, int nodeSize) {
			return (Queue<T>)queues.computeIfAbsent(name, key -> new Queue<>(this, key, valueClass, nodeSize));
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

	public boolean isEmpty() {
		var root = module._tQueues.get(name);
		return root == null || root.getHeadNodeId() == 0;
	}

	/**
	 * 删除并返回整个头节点
	 *
	 * @return 头节点，null if empty
	 */
	public BQueueNode pollNode() {
		var root = module._tQueues.get(name);
		if (root == null)
			return null;

		long headNodeId = root.getHeadNodeId();
		if (headNodeId == 0)
			return null;
		var nodeKey = new BQueueNodeKey(name, headNodeId);
		var head = module._tQueueNodes.get(nodeKey);
		if (head == null)
			return null;

		root.setHeadNodeId(head.getNextNodeId());
		root.setCount(root.getCount() - head.getValues().size());
		module._tQueueNodes.delayRemove(nodeKey);
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
		var root = module._tQueues.get(name);
		if (root == null)
			return null;

		long headNodeId = root.getHeadNodeId();
		if (headNodeId == 0)
			return null;
		return module._tQueueNodes.get(new BQueueNodeKey(name, headNodeId));
	}

	/**
	 * 删除并返回头节点中的首个值
	 *
	 * @return 头节点的首个值，null if empty
	 */
	public V poll() {
		var root = module._tQueues.get(name);
		if (root == null)
			return null;

		long headNodeId = root.getHeadNodeId();
		if (headNodeId == 0)
			return null;
		var nodeKey = new BQueueNodeKey(name, headNodeId);
		var head = module._tQueueNodes.get(nodeKey);
		if (head == null)
			return null;

		var nodeValues = head.getValues();
		var nodeValue = nodeValues.remove(0);
		root.setCount(root.getCount() - 1);
		if (nodeValues.isEmpty()) {
			root.setHeadNodeId(head.getNextNodeId());
			module._tQueueNodes.remove(nodeKey);
		}
		@SuppressWarnings("unchecked")
		var value = (V)nodeValue.getValue().getBean();
		return value;
	}

	/**
	 * @return 头节点的首个值，null if empty
	 */
	public V peek() {
		var root = module._tQueues.get(name);
		if (root == null)
			return null;

		long headNodeId = root.getHeadNodeId();
		if (headNodeId == 0)
			return null;
		var head = module._tQueueNodes.get(new BQueueNodeKey(name, headNodeId));
		if (head == null)
			return null;

		@SuppressWarnings("unchecked")
		var value = (V)head.getValues().get(0).getValue().getBean();
		return value;
	}

	public long size() {
		var root = module._tQueues.getOrAdd(name);
		return root.getCount();
	}

	/**
	 * 用作queue, 值追加到尾节点的最后, 满则追加一个尾节点。
	 */
	public void add(V value) {
		var root = module._tQueues.getOrAdd(name);
		var tailNodeId = root.getTailNodeId();
		var tail = tailNodeId != 0 ? module._tQueueNodes.get(new BQueueNodeKey(name, tailNodeId)) : null;
		if (tail == null || tail.getValues().size() >= nodeSize) {
			var newNodeId = root.getLastNodeId() + 1;
			root.setLastNodeId(newNodeId);
			root.setTailNodeId(newNodeId);
			if (root.getHeadNodeId() == 0)
				root.setHeadNodeId(newNodeId);
			if (tail != null)
				tail.setNextNodeId(newNodeId);
			module._tQueueNodes.insert(new BQueueNodeKey(name, newNodeId), tail = new BQueueNode());
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
		var root = module._tQueues.getOrAdd(name);
		var headNodeId = root.getHeadNodeId();
		var head = headNodeId != 0 ? module._tQueueNodes.get(new BQueueNodeKey(name, headNodeId)) : null;
		if (head == null || head.getValues().size() >= nodeSize) {
			var newNodeId = root.getLastNodeId() + 1;
			root.setLastNodeId(newNodeId);
			root.setHeadNodeId(newNodeId);
			if (root.getTailNodeId() == 0)
				root.setTailNodeId(newNodeId);
			module._tQueueNodes.insert(new BQueueNodeKey(name, newNodeId), head = new BQueueNode());
			root.setCount(root.getCount() + 1);
			if (headNodeId != 0)
				head.setNextNodeId(headNodeId);
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
	public long walk(TableWalkHandle<Long, V> func) {
		long count = 0L;
		var root = module._tQueues.selectDirty(name);
		if (null == root)
			return count;
		var nodeId = root.getHeadNodeId();
		while (nodeId != 0) {
			var node = module._tQueueNodes.selectDirty(new BQueueNodeKey(name, nodeId));
			if (null == node)
				return count;
			for (var value : node.getValues()) {
				++count;
				if (!func.handle(nodeId, (V)value.getValue().getBean()))
					return count;
			}
			nodeId = node.getNextNodeId();
		}
		return count;
	}
}
