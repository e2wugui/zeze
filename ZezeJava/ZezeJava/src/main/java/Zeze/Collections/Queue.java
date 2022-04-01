package Zeze.Collections;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Beans.Collections.Queue.BQueueNode;
import Zeze.Beans.Collections.Queue.BQueueNodeKey;
import Zeze.Beans.Collections.Queue.BQueueNodeValue;
import Zeze.Transaction.Bean;
import Zeze.Transaction.TableWalkHandle;

public class Queue<V extends Bean> {
	private static final BeanFactory beanFactory = new BeanFactory();

	public static long GetSpecialTypeIdFromBean(Bean bean) {
		return beanFactory.GetSpecialTypeIdFromBean(bean);
	}

	public static Bean CreateBeanFromSpecialTypeId(long typeId) {
		return beanFactory.CreateBeanFromSpecialTypeId(typeId);
	}

	public static class Module extends AbstractQueue {
		private final ConcurrentHashMap<String, Queue<?>> Queues = new ConcurrentHashMap<>();

		public Module(Zeze.Application zeze) {
			RegisterZezeTables(zeze);
		}

		@SuppressWarnings("unchecked")
		public <T extends Bean> Queue<T> open(String name, Class<T> valueClass) {
			return (Queue<T>)Queues.computeIfAbsent(name, key -> new Queue<>(this, key, valueClass, 100));
		}

		@SuppressWarnings("unchecked")
		public <T extends Bean> Queue<T> open(String name, Class<T> valueClass, int nodeSize) {
			return (Queue<T>)Queues.computeIfAbsent(name, key -> new Queue<>(this, key, valueClass, nodeSize));
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
		module._tQueueNodes.remove(nodeKey);
		return head;
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
	 * 必须在事务外。
	 * func 第一个参数是当前Value所在的Node.Id。
	 */
	@SuppressWarnings("unchecked")
	public void walk(TableWalkHandle<Long, V> func) {
		module._tQueueNodes.Walk((key, node) -> {
			for (var value : node.getValues()) {
				if (!func.handle(key.getNodeId(), (V)value.getValue().getBean()))
					return false;
			}
			return true;
		});
	}
}
