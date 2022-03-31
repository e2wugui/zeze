package Zeze.Collections;

import Zeze.Beans.Collections.Queue.BQueueNode;
import Zeze.Beans.Collections.Queue.BQueueNodeKey;
import Zeze.Beans.Collections.Queue.BQueueNodeValue;
import Zeze.Transaction.Bean;
import Zeze.Transaction.TableWalkHandle;

public class Queue<V extends Bean> extends AbstractQueue {
	public static long GetSpecialTypeIdFromBean(Bean bean) {
		return LinkedMap.GetSpecialTypeIdFromBean(bean);
	}

	public static Bean CreateBeanFromSpecialTypeId(long typeId) {
		return LinkedMap.CreateBeanFromSpecialTypeId(typeId);
	}

	private final String name;
	private final int nodeSize;
	private boolean init;

	public Queue(String name, Class<V> valueClass) {
		this(name, 50, valueClass);
	}

	public Queue(String name, int nodeSize, Class<V> valueClass) {
		this.name = name;
		this.nodeSize = nodeSize;
		LinkedMap.register(valueClass);
	}

	public String getName() {
		return name;
	}

	/**
	 * 必须在 Zeze.Start 之前调用。比较好的地方放在 App.Create 后面。
	 */
	public synchronized void initialize(Zeze.Application zeze) {
		if (init)
			return;
		RegisterZezeTables(zeze);
		init = true;
	}

	// 一般不需要调用
	public void finalize(Zeze.Application zeze) {
		if (!init)
			return;
		UnRegisterZezeTables(zeze);
		init = false;
	}

	/**
	 * 提取整个头节点
	 *
	 * @return 头节点，null if empty
	 */
	public BQueueNode pollNode() {
		var root = _tQueues.get(name);
		if (null == root)
			return null;

		var nodeKey = new BQueueNodeKey(name, root.getHeadNodeId());
		var head = _tQueueNodes.get(nodeKey);
		if (null == head)
			return null;

		root.setHeadNodeId(head.getNextNodeId());
		_tQueueNodes.remove(nodeKey);
		return head;
	}

	public BQueueNode peekNode() {
		var root = _tQueues.get(name);
		if (null == root)
			return null;

		var nodeKey = new BQueueNodeKey(name, root.getHeadNodeId());
		return _tQueueNodes.get(nodeKey); // head
	}

	/**
	 * 提取队列第一项
	 *
	 * @return 第一项的值，null if empty
	 */
	public V poll() {
		var root = _tQueues.get(name);
		if (null == root)
			return null;

		var nodeKey = new BQueueNodeKey(name, root.getHeadNodeId());
		var head = _tQueueNodes.get(nodeKey);
		if (null == head)
			return null;

		@SuppressWarnings("unchecked")
		var value = (V)head.getValues().remove(0);
		if (head.getValues().isEmpty()) {
			root.setHeadNodeId(head.getNextNodeId());
			_tQueueNodes.remove(nodeKey);
		}
		return value;
	}

	public V peek() {
		var root = _tQueues.get(name);
		if (null == root)
			return null;

		var nodeKey = new BQueueNodeKey(name, root.getHeadNodeId());
		var head = _tQueueNodes.get(nodeKey);
		if (null == head)
			return null;

		@SuppressWarnings("unchecked")
		var value = (V)head.getValues().remove(0);
		return value;
	}

	/**
	 * 加入值。
	 *
	 * @param value to add
	 */
	public void add(V value) {
		var root = _tQueues.getOrAdd(name);
		var tail = _tQueueNodes.get(new BQueueNodeKey(name, root.getTailNodeId()));
		if (null == tail || tail.getValues().size() > nodeSize) {
			var newNode = new BQueueNode();
			root.setLastNodeId(root.getLastNodeId() + 1);
			var newNodeId = root.getLastNodeId();
			_tQueueNodes.insert(new BQueueNodeKey(name, newNodeId), newNode);
			root.setTailNodeId(newNodeId);
			if (null != tail)
				tail.setNextNodeId(newNodeId);
			tail = newNode;
		}
		var nodeValue = new BQueueNodeValue();
		nodeValue.setTimestamp(System.currentTimeMillis());
		nodeValue.getValue().setBean(value);
		tail.getValues().add(nodeValue);
	}

	// stack
	public void push(V value) {
		var root = _tQueues.getOrAdd(name);
		var head = _tQueueNodes.get(new BQueueNodeKey(name, root.getHeadNodeId()));
		if (null == head || head.getValues().size() > nodeSize) {
			var newNode = new BQueueNode();
			root.setLastNodeId(root.getLastNodeId() + 1);
			var newNodeId = root.getLastNodeId();
			_tQueueNodes.insert(new BQueueNodeKey(name, newNodeId), newNode);
			newNode.setNextNodeId(root.getHeadNodeId());
			root.setHeadNodeId(newNodeId);
			head = newNode;
		}
		var nodeValue = new BQueueNodeValue();
		nodeValue.setTimestamp(System.currentTimeMillis());
		nodeValue.getValue().setBean(value);
		head.getValues().add(0, nodeValue);
	}

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
		_tQueueNodes.Walk((key, node) -> {
			for (var value : node.getValues()) {
				if (!func.handle(key.getNodeId(), (V)value.getValue().getBean()))
					return false;
			}
			return true;
		});
	}
}
