package Zeze.Collections;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMap;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeValue;
import Zeze.Transaction.Bean;
import Zeze.Transaction.TableWalkHandle;

public class LinkedMap<V extends Bean> {
	private static final BeanFactory beanFactory = new BeanFactory();

	public static long GetSpecialTypeIdFromBean(Bean bean) {
		return beanFactory.GetSpecialTypeIdFromBean(bean);
	}

	public static Bean CreateBeanFromSpecialTypeId(long typeId) {
		return beanFactory.CreateBeanFromSpecialTypeId(typeId);
	}

	public static class Module extends AbstractLinkedMap {
		private final ConcurrentHashMap<String, LinkedMap<?>> LinkedMaps = new ConcurrentHashMap<>();
		public final Zeze.Application Zeze;

		public Module(Zeze.Application zeze) {
			Zeze = zeze;
			RegisterZezeTables(zeze);
		}

		@Override
		public void UnRegister() {
			UnRegisterZezeTables(Zeze);
		}

		@SuppressWarnings("unchecked")
		public <T extends Bean> LinkedMap<T> open(String name, Class<T> valueClass, int nodeSize) {
			return (LinkedMap<T>)LinkedMaps.computeIfAbsent(name, k -> new LinkedMap<>(this, k, valueClass, nodeSize));
		}

		@SuppressWarnings("unchecked")
		public <T extends Bean> LinkedMap<T> open(String name, Class<T> valueClass) {
			return (LinkedMap<T>)LinkedMaps.computeIfAbsent(name, k -> new LinkedMap<>(this, k, valueClass, 100));
		}
	}

	private final Module module;
	private final String name;
	private final int nodeSize;

	private LinkedMap(Module module, String name, Class<V> valueClass, int nodeSize) {
		this.module = module;
		this.name = name;
		this.nodeSize = nodeSize;
		beanFactory.register(valueClass);
	}

	public String getName() {
		return name;
	}

	// list
	public BLinkedMap getRoot() {
		return module._tLinkedMaps.get(name);
	}

	public BLinkedMapNode getNode(long nodeId) {
		return module._tLinkedMapNodes.get(new BLinkedMapNodeKey(name, nodeId));
	}

	public BLinkedMapNode getFristNode() {
		var root = getRoot();
		if (null != root)
			return getNode(root.getHeadNodeId());
		return null;
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public long size() {
		return getRoot().getCount();
	}

	public long moveAhead(String id) {
		return move(id, true);
	}

	public long moveTail(String id) {
		return move(id, false);
	}

	private long move(String id, boolean ahead) {
		var nodeId = module._tValueIdToNodeId.get(new BLinkedMapKey(name, id));
		if (nodeId == null)
			return 0;

		var nodeIdLong = nodeId.getNodeId();
		var node = getNode(nodeIdLong);
		var values = node.getValues();

		// activate。优化：这个操作比较多，已经在目标位置，不调整。
		if (ahead) {
			if (values.get(0).getId().equals(id) && getRoot().getHeadNodeId() == nodeIdLong) // HeadNode && List.Last
				return nodeIdLong;
		} else {
			if (values.get(values.size() - 1).getId().equals(id) && getRoot().getTailNodeId() == nodeIdLong) // TailNode && List.Last
				return nodeIdLong;
		}

		for (int i = 0; i < values.size(); i++) {
			var e = values.get(i);
			if (e.getId().equals(id)) {
				values.remove(i);
				if (values.isEmpty())
					removeNodeUnsafe(nodeId.getNodeId(), node);
				return ahead ? addHeadUnsafe(e.Copy()) : addTailUnsafe(e.Copy());
			}
		}
		throw new IllegalStateException("Node Exist But Value Not Found.");
	}

	// map
	public V put(long id, V value) {
		return put(String.valueOf(id), value, true);
	}

	public V put(String id, V value) {
		return put(id, value, true);
	}

	public V put(String id, V value, boolean ahead) {
		var nodeIdKey = new BLinkedMapKey(name, id);
		var nodeId = module._tValueIdToNodeId.get(nodeIdKey);
		if (nodeId == null) {
			var newNodeValue = new BLinkedMapNodeValue();
			newNodeValue.setId(id);
			newNodeValue.getValue().setBean(value);
			nodeId = new BLinkedMapNodeId();
			nodeId.setNodeId(ahead ? addHeadUnsafe(newNodeValue) : addTailUnsafe(newNodeValue));
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

	public V get(String id) {
		var nodeId = module._tValueIdToNodeId.get(new BLinkedMapKey(name, id));
		if (nodeId == null)
			return null;

		var node = getNode(nodeId.getNodeId());
		for (var e : node.getValues()) {
			if (e.getId().equals(id)) {
				@SuppressWarnings("unchecked")
				var value = (V)e.getValue().getBean();
				return value;
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
		if (nodeId == null)
			return null;

		var node = getNode(nodeId.getNodeId());
		var values = node.getValues();
		for (int i = 0, n = values.size(); i < n; i++) {
			var e = values.get(i);
			if (e.getId().equals(id)) {
				values.remove(i);
				module._tValueIdToNodeId.remove(nodeKey);
				var root = getRoot();
				root.setCount(root.getCount() - 1);
				if (values.isEmpty())
					removeNodeUnsafe(nodeId.getNodeId(), node);
				return (V)e.getValue().getBean();
			}
		}
		throw new IllegalStateException("NodeId Exist. But Value Not Found.");
	}

	public void removeNode(long nodeId) {
		BLinkedMapNode node = getNode(nodeId);
		for (var e : node.getValues()) {
			module._tValueIdToNodeId.remove(new BLinkedMapKey(name, e.getId()));
			var root = getRoot();
			root.setCount(root.getCount() - node.getValues().size());
			node.getValues().clear();
			removeNodeUnsafe(nodeId, node);
		}
		// 没有马上删除，启动gc延迟删除。
		module._tLinkedMapNodes.delayRemove(new BLinkedMapNodeKey(name, nodeId));
	}

	// foreach
	public void clear() {
		Zeze.Util.Task.run(() -> {
			var root = module._tLinkedMaps.selectDirty(name);
			if (null == root)
				return;

			var nodeId = root.getTailNodeId();
			while (nodeId != 0) {
				var node = module._tLinkedMapNodes.selectDirty(new BLinkedMapNodeKey(name, nodeId));
				final var finalNodeId = nodeId;
				module.Zeze.NewProcedure(() -> {
					removeNode(finalNodeId);
					return 0;
				}, name + ".clear.node").Call();
				nodeId = node.getPrevNodeId();
			}
			module.Zeze.NewProcedure(() -> {
				module._tLinkedMaps.remove(name);
				return 0;
			}, name + ".clear.root").Call();
		}, name + ".clear");
	}

	@SuppressWarnings("unchecked")
	public long walk(TableWalkHandle<String, V> func) {
		long count = 0L;
		var root = module._tLinkedMaps.selectDirty(name);
		if (null == root)
			return count;

		var nodeId = root.getTailNodeId();
		while (nodeId != 0) {
			var node = module._tLinkedMapNodes.selectDirty(new BLinkedMapNodeKey(name, nodeId));
			if (null == node)
				return count; // error

			for (var value : node.getValues()) {
				++count;
				if (!func.handle(value.getId(), (V)value.getValue().getBean()))
					return count;
			}
			nodeId = node.getPrevNodeId();
		}
		return count;
	}

	// inner
	private long addHeadUnsafe(BLinkedMapNodeValue nodeValue) {
		var root = module._tLinkedMaps.getOrAdd(name);
		var headNodeId = root.getHeadNodeId();
		var head = headNodeId != 0 ? getNode(headNodeId) : null;
		if (head != null && head.getValues().size() < nodeSize) {
			// head is null means empty
			head.getValues().add(0, nodeValue);
			return headNodeId;
		}
		var newNode = new BLinkedMapNode();
		if (headNodeId != 0)
			newNode.setNextNodeId(headNodeId); // 这里包含了empty
		newNode.getValues().add(0, nodeValue);
		var newNodeId = root.getLastNodeId() + 1;
		root.setLastNodeId(newNodeId);
		root.setHeadNodeId(newNodeId);
		module._tLinkedMapNodes.insert(new BLinkedMapNodeKey(name, newNodeId), newNode);
		if (head != null)
			head.setPrevNodeId(newNodeId);
		else // isEmpty.
			root.setTailNodeId(newNodeId);
		return newNodeId;
	}

	private long addTailUnsafe(BLinkedMapNodeValue nodeValue) {
		var root = module._tLinkedMaps.getOrAdd(name);
		var tailNodeId = root.getTailNodeId();
		var tail = tailNodeId != 0 ? getNode(tailNodeId) : null;
		if (tail != null && tail.getValues().size() < nodeSize) { // tail is null means empty
			tail.getValues().add(nodeValue);
			return tailNodeId;
		}
		var newNode = new BLinkedMapNode();
		if (tailNodeId != 0)
			newNode.setPrevNodeId(tailNodeId); // 这里包含了empty
		newNode.getValues().add(nodeValue);
		var newNodeId = root.getLastNodeId() + 1;
		root.setLastNodeId(newNodeId);
		root.setTailNodeId(newNodeId);
		module._tLinkedMapNodes.insert(new BLinkedMapNodeKey(name, newNodeId), newNode);
		if (tail != null)
			tail.setNextNodeId(newNodeId);
		else // isEmpty.
			root.setHeadNodeId(newNodeId);
		return newNodeId;
	}

	private void removeNodeUnsafe(long nodeId, BLinkedMapNode node) {
		var root = getRoot();
		var prevNodeId = node.getPrevNodeId();
		var nextNodeId = node.getNextNodeId();

		if (prevNodeId == 0) // is head
			root.setHeadNodeId(nextNodeId);
		else
			getNode(prevNodeId).setNextNodeId(nextNodeId);

		if (nextNodeId == 0) // is tail
			root.setTailNodeId(prevNodeId);
		else
			getNode(nextNodeId).setPrevNodeId(prevNodeId);

		// 没有马上删除，启动gc延迟删除。
		module._tLinkedMapNodes.delayRemove(new BLinkedMapNodeKey(name, nodeId));
	}
}
