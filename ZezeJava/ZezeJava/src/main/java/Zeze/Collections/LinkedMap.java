package Zeze.Collections;

import java.lang.invoke.MethodHandle;
import Zeze.Beans.Collections.LinkedMap.BLinkedMapKey;
import Zeze.Beans.Collections.LinkedMap.BLinkedMapNode;
import Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeId;
import Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeKey;
import Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeValue;
import Zeze.Transaction.Bean;
import Zeze.Util.LongHashMap;
import Zeze.Util.Reflect;

public class LinkedMap<V extends Bean> {
	private static final Module module = new Module();

	public static Module getModule() {
		return module;
	}

	public static long GetSpecialTypeIdFromBean(Bean bean) {
		return module.GetSpecialTypeIdFromBean(bean);
	}

	public static Bean CreateBeanFromSpecialTypeId(long typeId) {
		return module.CreateBeanFromSpecialTypeId(typeId);
	}

	public static class Module extends AbstractLinkedMap {
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

	public LinkedMap(String name, Class<V> valueClass) {
		this(name, 100, valueClass);
	}

	public LinkedMap(String name, int nodeSize, Class<V> valueClass) {
		this.name = name;
		this.nodeSize = nodeSize;
		module.register(valueClass);
	}

	public String getName() {
		return name;
	}

	// LinkedMapNode
	public BLinkedMapNode getNode(long cur) {
		return module._tLinkedMapNodes.get(new BLinkedMapNodeKey(name, cur));
	}

	// list order
	/**
	 * 把项移到队尾。
	 * 如果它不是pin的，那么就是pin的之前的第一个。
	 * @param id of value
	 */
	public void activate(String id) {
		var nodeKey = new BLinkedMapKey(name, id);
		var nodeId = module._tValueIdToNodeId.get(nodeKey);
		if (null == nodeId)
			return;
		var node = getNode(nodeId.getNodeId());
		for (int i = 0; i < node.getValues().size(); ++i) {
			var e = node.getValues().get(i);
			if (e.getId().equals(id)) {
				// activate。优化：这个操作比较多，并且很可能都是尾部活跃，需要判断已经是最后一个了，不调整。
				var root = module._tLinkedMaps.get(name);
				if (e.isPin()) {
					if (root.getTailNodeId() == nodeId.getNodeId() && i == node.getValues().size() - 1)
						return; // TailNode && List.Last
				} else if (root.getLastNotPinNodeId() == nodeId.getNodeId()) {
					if (i == node.getValues().size() - 1)
						return; // LastNotPin && List.Last
					if (node.getValues().get(i + 1).isPin())
						return; // LastNotPin && Next Is Pin
				}
				node.getValues().remove(i);
				nodeId.setNodeId(addUnsafe(e.Copy()));
				if (node.getValues().isEmpty())
					removeNodeUnsafe(nodeKey, nodeId, node);
				return;
			}
		}
	}

	public void pin(String id, boolean value) {
		var nodeKey = new BLinkedMapKey(name, id);
		var nodeId = module._tValueIdToNodeId.get(nodeKey);
		if (null == nodeId)
			return;
		var node = getNode(nodeId.getNodeId());
		for (int i = 0; i < node.getValues().size(); ++i) {
			var e = node.getValues().get(i);
			if (e.getId().equals(id)) {
				e.setPin(value);
				if (value) {
					// activate
					node.getValues().remove(i);
					nodeId.setNodeId(addUnsafe(e.Copy()));
					if (node.getValues().isEmpty())
						removeNodeUnsafe(nodeKey, nodeId, node);
				}
				return;
			}
		}
	}

	// map
	public V put(long id, V value) {
		return put(String.valueOf(id), value);
	}

	public V put(String id, V value) {
		var nodeId = module._tValueIdToNodeId.get(new BLinkedMapKey(name, id));
		if (null == nodeId) {
			var newNodeValue = new BLinkedMapNodeValue();
			newNodeValue.setId(id);
			newNodeValue.getValue().setBean(value);
			addUnsafe(newNodeValue);
			var root = module._tLinkedMaps.get(name);
			root.setCount(root.getCount() + 1);
			return null;
		}
		var node = getNode(nodeId.getNodeId());
		for (var e : node.getValues()) {
			if (e.getId().equals(id)) {
				var old = (V)e.getValue().getBean();
				e.getValue().setBean(value);
				return old;
			}
		}
		throw new RuntimeException("NodeId Exist. But Value Not Found.");
	}

	public V get(long id) {
		return get(String.valueOf(id));
	}

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
				var root = module._tLinkedMaps.get(name);
				root.setCount(root.getCount() - 1);
				if (node.getValues().isEmpty())
					removeNodeUnsafe(nodeKey, nodeId, node);
				return (V)e.getValue().getBean();
			}
		}
		throw new RuntimeException("NodeId Exist. But Value Not Found.");
	}

	// inner
	private long addUnsafe(BLinkedMapNodeValue nodeValue) {
		var root = module._tLinkedMaps.get(name);
		boolean sameTailAndLastNotPin = root.getTailNodeId() == root.getLastNotPinNodeId();

		if (nodeValue.isPin()) {
			var tail = getNode(root.getTailNodeId());
			if (tail.getValues().size() < nodeSize) {
				tail.getValues().add(nodeValue);
				return root.getTailNodeId();
			}
			var newNode = new BLinkedMapNode();
			newNode.getValues().add(nodeValue);
			root.setLastNodeId(root.getLastNodeId() + 1);
			var newNodeId = root.getLastNodeId();
			module._tLinkedMapNodes.insert(new BLinkedMapNodeKey(name, newNodeId), newNode);
			newNode.setPrevNodeId(root.getTailNodeId());
			root.setTailNodeId(newNodeId);
			newNode.setNextNodeId(tail.getNextNodeId());
			tail.setNextNodeId(newNodeId);
			return newNodeId;
		}
		// 注：这两段代码很像，但没必要重用。
		var last = getNode(root.getLastNotPinNodeId());
		if (last.getValues().size() < nodeSize) {
			int i = last.getValues().size() - 1;
			for (; i >= 0; --i)
				if (!last.getValues().get(i).isPin())
					break;
			last.getValues().add(++i, nodeValue);
			return root.getLastNotPinNodeId();
		}
		var newNode = new BLinkedMapNode();
		newNode.getValues().add(nodeValue);
		root.setLastNodeId(root.getLastNodeId() + 1);
		var newNodeId = root.getLastNodeId();
		module._tLinkedMapNodes.insert(new BLinkedMapNodeKey(name, newNodeId), newNode);
		newNode.setPrevNodeId(root.getLastNotPinNodeId());
		root.setLastNotPinNodeId(newNodeId);
		newNode.setNextNodeId(last.getNextNodeId());
		last.setNextNodeId(newNodeId);
		return newNodeId;
	}

	private void removeNodeUnsafe(BLinkedMapKey nodeKey, BLinkedMapNodeId nodeId, BLinkedMapNode node) {
		var root = module._tLinkedMaps.get(name);
		if (node.getPrevNodeId() == nodeId.getNodeId() || node.getNextNodeId() == nodeId.getNodeId()) {
			// single node。
			root.setHeadNodeId(0);
			root.setTailNodeId(0);
			root.setLastNotPinNodeId(0);
			// GC TODO
			return;
		}
		var prevNode = getNode(node.getPrevNodeId());
		var nextNode = getNode(node.getNextNodeId());
		nextNode.setPrevNodeId(node.getPrevNodeId());
		prevNode.setNextNodeId(node.getNextNodeId());
		if (nodeId.getNodeId() == root.getTailNodeId()) {
			root.setTailNodeId(node.getPrevNodeId());
		}
		// GC TODO
	}
}
