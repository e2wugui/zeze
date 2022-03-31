package Zeze.Collections;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Beans.Collections.LinkedMap.BLinkedMap;
import Zeze.Beans.Collections.LinkedMap.BLinkedMapKey;
import Zeze.Beans.Collections.LinkedMap.BLinkedMapNode;
import Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeId;
import Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeKey;
import Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeValue;
import Zeze.Transaction.Bean;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Util.FastRWLock;
import Zeze.Util.LongHashMap;
import Zeze.Util.Reflect;

public class LinkedMap<V extends Bean> {
	private static final LongHashMap<MethodHandle> factory = new LongHashMap<>();
	private static final FastRWLock factoryLock = new FastRWLock();

	public static void register(Class<? extends Bean> beanClass) {
		MethodHandle beanCtor = Reflect.getDefaultConstructor(beanClass);
		Bean bean;
		try {
			bean = (Bean)beanCtor.invoke();
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		factoryLock.writeLock();
		try {
			factory.putIfAbsent(bean.getTypeId(), beanCtor);
		} finally {
			factoryLock.writeUnlock();
		}
	}

	public static long GetSpecialTypeIdFromBean(Bean bean) {
		return bean.getTypeId();
	}

	public static Bean CreateBeanFromSpecialTypeId(long typeId) {
		MethodHandle beanCtor;
		factoryLock.readLock();
		try {
			beanCtor = factory.get(typeId);
		} finally {
			factoryLock.readUnlock();
		}
		if (beanCtor == null)
			throw new UnsupportedOperationException("Unknown Bean TypeId=" + typeId);
		try {
			return (Bean)beanCtor.invoke();
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static class Module extends AbstractLinkedMap {
		public Module(Zeze.Application zeze) {
			RegisterZezeTables(zeze);
		}

		public <T extends Bean> LinkedMap<T> open(String name, Class<T> valueClass, int nodeSize) {
			return (LinkedMap<T>)LinkedMaps.computeIfAbsent(name, (key) -> new LinkedMap<T>(this, name, valueClass, nodeSize));
		}

		public <T extends Bean> LinkedMap<T> open(String name, Class<T> valueClass) {
			return (LinkedMap<T>)LinkedMaps.computeIfAbsent(name, (key) -> new LinkedMap<T>(this, name, valueClass, 100));
		}

		private ConcurrentHashMap<String, LinkedMap<?>> LinkedMaps = new ConcurrentHashMap<>();
	}

	private final Module module;
	private final String name;
	private final int nodeSize;
	private boolean init;

	private LinkedMap(Module module, String name, Class<V> valueClass, int nodeSize) {
		this.module = module;
		this.name = name;
		this.nodeSize = nodeSize;
		register(valueClass);
	}

	public String getName() {
		return name;
	}

	// list
	public BLinkedMap getRoot() {
		return module._tLinkedMaps.get(name);
	}

	public BLinkedMapNode getNode(long cur) {
		return module._tLinkedMapNodes.get(new BLinkedMapNodeKey(name, cur));
	}

	/**
	 * 把项移到队尾。
	 *
	 * @param id of value
	 * @return node id that contains value
	 */
	public long moveToTail(String id) {
		var nodeKey = new BLinkedMapKey(name, id);
		var nodeId = module._tValueIdToNodeId.get(nodeKey);
		if (null == nodeId)
			return 0;
		var node = getNode(nodeId.getNodeId());
		for (int i = 0; i < node.getValues().size(); ++i) {
			var e = node.getValues().get(i);
			if (e.getId().equals(id)) {
				// activate。优化：这个操作比较多，并且很可能都是尾部活跃，需要判断已经是最后一个了，不调整。
				var root = getRoot();
				if (root.getTailNodeId() == nodeId.getNodeId() && i == node.getValues().size() - 1)
					return nodeId.getNodeId(); // TailNode && List.Last
				node.getValues().remove(i);
				nodeId.setNodeId(addUnsafe(e.Copy()));
				if (node.getValues().isEmpty())
					removeNodeUnsafe(nodeId, node);
				return nodeId.getNodeId();
			}
		}
		throw new IllegalStateException("Node Exist But Value Not Found.");
	}

	// map
	public V put(long id, V value) {
		return put(String.valueOf(id), value);
	}

	public V put(String id, V value) {
		var nodeIdKey = new BLinkedMapKey(name, id);
		var nodeId = module._tValueIdToNodeId.get(nodeIdKey);
		if (null == nodeId) {
			var newNodeValue = new BLinkedMapNodeValue();
			newNodeValue.setId(id);
			newNodeValue.getValue().setBean(value);
			nodeId = new BLinkedMapNodeId();
			nodeId.setNodeId(addUnsafe(newNodeValue));
			module._tValueIdToNodeId.insert(nodeIdKey, nodeId);
			var root = getRoot();
			root.setCount(root.getCount() + 1);
			return null;
		}
		var node = getNode(nodeId.getNodeId());
		for (var e : node.getValues()) {
			if (e.getId().equals(id)) {
				@SuppressWarnings("unchecked")
				var old = (V)e.getValue().getBean();
				e.getValue().setBean(value);
				return old;
			}
		}
		throw new IllegalStateException("NodeId Exist. But Value Not Found.");
	}

	public V get(long id) {
		return get(String.valueOf(id));
	}

	@SuppressWarnings("unchecked")
	public V get(String id) {
		var nodeId = module._tValueIdToNodeId.get(new BLinkedMapKey(name, id));
		if (null == nodeId) {
			return null;
		}
		var node = getNode(nodeId.getNodeId());
		for (var e : node.getValues()) {
			if (e.getId().equals(id)) {
				return (V)e.getValue().getBean();
			}
		}
		return null;
	}

	public V remove(long id) {
		return remove(String.valueOf(id));
	}

	@SuppressWarnings("unchecked")
	public V remove(String id) {
		var nodeKey = new BLinkedMapKey(name, id);
		var nodeId = module._tValueIdToNodeId.get(nodeKey);
		if (null == nodeId) {
			return null;
		}
		var node = getNode(nodeId.getNodeId());
		for (int i = 0; i < node.getValues().size(); ++i) {
			var e = node.getValues().get(i);
			if (e.getId().equals(id)) {
				node.getValues().remove(i);
				module._tValueIdToNodeId.remove(nodeKey);
				var root = getRoot();
				root.setCount(root.getCount() - 1);
				if (node.getValues().isEmpty())
					removeNodeUnsafe(nodeId, node);
				return (V)e.getValue().getBean();
			}
		}
		throw new IllegalStateException("NodeId Exist. But Value Not Found.");
	}

	// foreach

	/**
	 * 必须在事务外。
	 * func 第一个参数是当前Value所在的Node.Id。
	 */
	@SuppressWarnings("unchecked")
	public void walk(TableWalkHandle<Long, V> func) {
		module._tLinkedMapNodes.Walk((key, node) -> {
			for (var value : node.getValues()) {
				if (!func.handle(key.getNodeId(), (V)value.getValue().getBean()))
					return false;
			}
			return true;
		});
	}

	// inner
	private long addUnsafe(BLinkedMapNodeValue nodeValue) {
		var root = module._tLinkedMaps.getOrAdd(name);
		var tail = getNode(root.getTailNodeId());
		// tail is null means empty
		if (null != tail && tail.getValues().size() < nodeSize) {
			tail.getValues().add(nodeValue);
			return root.getTailNodeId();
		}
		var newNode = new BLinkedMapNode();
		newNode.getValues().add(nodeValue);
		root.setLastNodeId(root.getLastNodeId() + 1);
		var newNodeId = root.getLastNodeId();
		module._tLinkedMapNodes.insert(new BLinkedMapNodeKey(name, newNodeId), newNode);
		newNode.setPrevNodeId(root.getTailNodeId()); // 这里包含了empty
		root.setTailNodeId(newNodeId);
		if (null != tail) {
			newNode.setNextNodeId(tail.getNextNodeId());
			tail.setNextNodeId(newNodeId);
		} else {
			// isEmpty.
			newNode.setNextNodeId(root.getHeadNodeId());
			root.setHeadNodeId(newNodeId);
		}
		return newNodeId;
	}

	private void removeNodeUnsafe(BLinkedMapNodeId nodeId, BLinkedMapNode node) {
		var root = getRoot();
		if (node.getPrevNodeId() == 0) {
			// is head
			root.setHeadNodeId(node.getNextNodeId());
		} else {
			var prevNode = getNode(node.getPrevNodeId());
			prevNode.setNextNodeId(node.getNextNodeId());
		}
		if (node.getNextNodeId() == 0) {
			// is tail
			root.setTailNodeId(node.getPrevNodeId());
		} else {
			var nextNode = getNode(node.getNextNodeId());
			nextNode.setPrevNodeId(node.getPrevNodeId());
		}

		// 没有马上删除，启动gc延迟删除。
		module._tLinkedMapNodes.delayRemove(new BLinkedMapNodeKey(name, nodeId.getNodeId()));
	}
}
