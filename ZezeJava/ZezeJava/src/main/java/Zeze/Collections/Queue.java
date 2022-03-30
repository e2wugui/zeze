package Zeze.Collections;

import java.lang.invoke.MethodHandle;
import Zeze.Beans.Collections.Queue.BQueueNode;
import Zeze.Beans.Collections.Queue.BQueueNodeKey;
import Zeze.Beans.Collections.Queue.BQueueNodeValue;
import Zeze.Transaction.Bean;
import Zeze.Util.LongHashMap;
import Zeze.Util.Reflect;

public class Queue<V extends Bean> {
	private static final Queue.Module module = new Queue.Module();

	public static Queue.Module getModule() {
		return module;
	}

	public static long GetSpecialTypeIdFromBean(Bean bean) {
		return module.GetSpecialTypeIdFromBean(bean);
	}

	public static Bean CreateBeanFromSpecialTypeId(long typeId) {
		return module.CreateBeanFromSpecialTypeId(typeId);
	}

	public static class Module extends AbstractQueue {
		private static final LongHashMap<MethodHandle> factory = new LongHashMap<>();

		private boolean init;

		public void register(Class<? extends Bean> beanClass) {
			MethodHandle beanCtor = Reflect.getDefaultConstructor(beanClass);
			Bean bean;
			try {
				bean = (Bean)beanCtor.invoke();
			} catch (RuntimeException e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
			synchronized (factory) {
				factory.putIfAbsent(bean.getTypeId(), beanCtor);
			}
		}

		/**
		 * 必须在 Zeze.Start 之前调用。比较好的地方放在 App.Create 后面。
		 */
		public void initialize(Zeze.Application zeze) {
			synchronized (module) {
				if (init)
					return;
				module.RegisterZezeTables(zeze);
				init = true;
			}
		}

		public long GetSpecialTypeIdFromBean(Bean bean) {
			return bean.getTypeId();
		}

		public Bean CreateBeanFromSpecialTypeId(long typeId) {
			MethodHandle beanCtor;
			synchronized (factory) {
				beanCtor = factory.get(typeId);
			}
			if (beanCtor == null)
				throw new RuntimeException("Unknown Bean TypeId=" + typeId);
			try {
				return (Bean)beanCtor.invoke();
			} catch (RuntimeException e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}

		// 一般不需要调用
		public void finalize(Zeze.Application zeze) {
			synchronized (module) {
				if (!init)
					return;
				module.UnRegisterZezeTables(zeze);
				init = false;
			}
		}
	}

	private final String name;
	private final int nodeSize;

	public Queue(String name, Class<V> valueClass) {
		this(name, 50, valueClass);
	}

	public Queue(String name, int nodeSize, Class<V> valueClass) {
		this.name = name;
		this.nodeSize = nodeSize;
		module.register(valueClass);
	}

	public String getName() {
		return name;
	}

	/**
	 * 提取整个头节点
	 * @return 头节点，null if empty
	 */
	public BQueueNode pollNode() {
		var root = module._tQueues.get(name);
		if (null == root)
			return null;

		var nodeKey = new BQueueNodeKey(name, root.getHeadNodeId());
		var head = module._tQueueNodes.get(nodeKey);
		if (null == head)
			return null;

		root.setHeadNodeId(head.getNextNodeId());
		module._tQueueNodes.remove(nodeKey);
		return head;
	}

	public BQueueNode peekNode() {
		var root = module._tQueues.get(name);
		if (null == root)
			return null;

		var nodeKey = new BQueueNodeKey(name, root.getHeadNodeId());
		var head = module._tQueueNodes.get(nodeKey);
		return head;
	}

	/**
	 * 提取队列第一项
	 * @return 第一项的值，null if empty
	 */
	public V poll() {
		var root = module._tQueues.get(name);
		if (null == root)
			return null;

		var nodeKey = new BQueueNodeKey(name, root.getHeadNodeId());
		var head = module._tQueueNodes.get(nodeKey);
		if (null == head)
			return null;

		var value = (V)head.getValues().remove(0);
		if (head.getValues().isEmpty()) {
			root.setHeadNodeId(head.getNextNodeId());
			module._tQueueNodes.remove(nodeKey);
		}
		return value;
	}

	public V peek() {
		var root = module._tQueues.get(name);
		if (null == root)
			return null;

		var nodeKey = new BQueueNodeKey(name, root.getHeadNodeId());
		var head = module._tQueueNodes.get(nodeKey);
		if (null == head)
			return null;

		var value = (V)head.getValues().remove(0);
		return value;
	}

	/**
	 * 加入值。
	 * @param value to add
	 */
	public void add(V value) {
		var root = module._tQueues.getOrAdd(name);
		var tail = module._tQueueNodes.get(new BQueueNodeKey(name, root.getTailNodeId()));
		if (null == tail || tail.getValues().size() > nodeSize) {
			var newNode = new BQueueNode();
			root.setLastNodeId(root.getLastNodeId() + 1);
			var newNodeId = root.getLastNodeId();
			module._tQueueNodes.insert(new BQueueNodeKey(name, newNodeId), newNode);
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
}
