package Zeze.Collections;

import Zeze.Beans.Collections.LinkedMap.*;
import Zeze.Transaction.Bean;

public class LinkedMap<V extends Bean> {
	public static long GetSpecialTypeIdFromBean(Bean bean) {
		return module.GetSpecialTypeIdFromBean(bean);
	}

	public static Bean CreateBeanFromSpecialTypeId(long typeId) {
		return module.CreateBeanFromSpecialTypeId(typeId);
	}

	public static class Module extends AbstractLinkedMap {
		private boolean init = false;

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
			var _typeId_ = bean.getTypeId();
			if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)
				return Zeze.Transaction.EmptyBean.TYPEID;
			throw new RuntimeException("Unknown Bean! dynamic@Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeValue:Value");
		}

		public Bean CreateBeanFromSpecialTypeId(long typeId) {
			return null;
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

	private final static Module module = new Module();

	public static Module getModule() {
		return module;
	}

	private final String name;
	private final int nodeSize;
	public String getName() {
		return name;
	}

	public LinkedMap(String name) {
		this(name, 100);
	}

	public LinkedMap(String name, int nodeSize) {
		this.name = name;
		this.nodeSize = nodeSize;
	}

	// LinkedMapNode
	public BLinkedMapNode getNode(long cur) {
		return module._tLinkedMapNodes.get(new BLinkedMapNodeKey(name, cur));
	}

	public BLinkedMapNode getNextNode(long cur) {
		var curNode = getNode(cur);
		return getNode(curNode.getNextNodeId());
	}

	public BLinkedMapNode getPrevNode(long cur) {
		var curNode = getNode(cur);
		return getNode(curNode.getPrevNodeId());
	}

	// list order
	/**
	 * 把项移到队尾。
	 * 如果它不是pin的，那么就是pin的之前的第一个。
	 * @param id of value
	 */
	public void activate(String id) {
		var nodeId = module._tValueIdToNodeId.get(new BLinkedMapKey(name, id));
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
					removeNodeUnsafe(nodeId, node);
				return;
			}
		}
	}

	public void pin(String id, boolean value) {
		var nodeId = module._tValueIdToNodeId.get(new BLinkedMapKey(name, id));
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
						removeNodeUnsafe(nodeId, node);
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
				if (node.getValues().isEmpty())
					removeNodeUnsafe(nodeId, node);
				return (V)e.getValue().getBean();
			}
		}
		throw new RuntimeException("NodeId Exist. But Value Not Found.");
	}

	// inner
	private long addUnsafe(BLinkedMapNodeValue nodeValue) {
		var root = module._tLinkedMaps.get(name);
		if (nodeValue.isPin()) {
			var tail = getNode(root.getTailNodeId());
			if (tail.getValues().size() < nodeSize) {
				tail.getValues().add(nodeValue);
				return root.getTailNodeId();
			}
			var newNode = new BLinkedMapNode();
			newNode.getValues().add(nodeValue);
			var newNodeId = 0L; // AutoKey
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
		var newNodeId = 0L; // AutoKey
		module._tLinkedMapNodes.insert(new BLinkedMapNodeKey(name, newNodeId), newNode);
		newNode.setPrevNodeId(root.getLastNotPinNodeId());
		root.setLastNotPinNodeId(newNodeId);
		newNode.setNextNodeId(last.getNextNodeId());
		last.setNextNodeId(newNodeId);
		return newNodeId;
	}

	private void removeNodeUnsafe(BLinkedMapNodeId nodeId, BLinkedMapNode node) {
		// GC TODO
		// 通用的记录垃圾回收机制，保存 List<(TableName, Key)>。
		// 比较缓慢的回收。比如：7天前。
	}
}
