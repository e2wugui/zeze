package Zeze.Collections;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Collections.LinkedMap.BClearJobState;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMap;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeReadOnly;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeValue;
import Zeze.Component.DelayRemove;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.ChangeListener;
import Zeze.Transaction.Changes;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Transaction.Transaction;
import Zeze.Util.OutLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 持久化链表 Map：按 id 访问的有序 map，节点分片存储、支持链表序分页读取与 O(1) clear。
 */
public class LinkedMap<V extends Bean> {
	private static final Logger logger = LogManager.getLogger(LinkedMap.class);
	public static final BeanFactory beanFactory = new BeanFactory();

	public static long getSpecialTypeIdFromBean(Serializable bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public static class Module extends AbstractLinkedMap {
		private final ConcurrentHashMap<String, Object> linkedMaps = new ConcurrentHashMap<>();
		public final Zeze.Application zeze;
		public static final String eClearJobHandleName = "Zeze.Collections.LinkedMap.Clear";
		// delayClearJob 每事务批量清理的节点数。
		public static final int clearJobBatchNodes = 16;
		private static final int concurrentBuckets = 128;
		// 公共名字禁止'@'，内部桶名以非空业务名字开头，所以该元数据名字不能与业务根/桶冲突。
		private static final String concurrentLayoutPrefix = "@CHashMap.Layout/";

		public Module(Zeze.Application zeze) {
			this.zeze = zeze;
			RegisterZezeTables(zeze);

			// 总是监听，但不直接开放。
			// 监听回调按LinkedMap.Name的后缀名进行回调，不支持广播。
			_tLinkedMapNodes.getChangeListenerMap().addListener(this::OnLinkedMapNodeChange);
			_tLinkedMaps.getChangeListenerMap().addListener(this::OnLinkedMapRootChange);

			this.zeze.getDelayRemove().register(eClearJobHandleName, this::delayClearJob);
		}

		public ByteBuffer encodeChangeListenerWithSpecialName(@Nullable String specialName,
																	   Object key, Changes.Record r) {
			return _tLinkedMapNodes.encodeChangeListenerWithSpecialName(specialName, key, r);
		}

		private void OnLinkedMapNodeChange(Object key, Changes.Record r) {
			var nodeKey = (BLinkedMapNodeKey)key;
			var indexOf = nodeKey.getName().lastIndexOf('@');
			if (indexOf >= 0) {
				var endsWith = nodeKey.getName().substring(indexOf);
				var listener = NodeListeners.get(endsWith);
				if (null != listener)
					listener.OnChanged(key, r);
			}
		}

		private void OnLinkedMapRootChange(Object key, Changes.Record r) {
			var name = (String)key;
			var indexOf = name.lastIndexOf('@');
			if (indexOf >= 0) {
				var endsWith = name.substring(indexOf);
				var listener = RootListeners.get(endsWith);
				if (null != listener)
					listener.OnChanged(key, r);
			}
		}

		@Override
		public void UnRegister() {
			UnRegisterZezeTables(zeze);
		}

		@SuppressWarnings("unchecked")
		<T extends Bean> LinkedMap<T> _open(String name, Class<T> valueClass, int nodeSize) {
			if (nodeSize < 1)
				throw new IllegalArgumentException("nodeSize < 1");
			if (name.isEmpty())
				throw new IllegalArgumentException("name is empty.");

			return (LinkedMap<T>)linkedMaps.computeIfAbsent(name, k -> new LinkedMap<>(this, k, valueClass, nodeSize));
		}

		public <T extends Bean> LinkedMap<T> open(String name, Class<T> valueClass, int nodeSize) {
			if (name.contains("@"))
				throw new IllegalArgumentException("name contains '@', that is reserved.");
			return _open(name, valueClass, nodeSize);
		}

		/**
		 * 打开按名称后缀派发变更的普通 LinkedMap。持久化名称为 owner + suffix。
		 * suffix 包含开头的 '@'，监听器使用同一后缀注册。
		 */
		public <T extends Bean> LinkedMap<T> openWithListenerSuffix(String owner, String suffix,
																  Class<T> valueClass, int nodeSize) {
			if (owner.isEmpty() || owner.startsWith(concurrentLayoutPrefix))
				throw new IllegalArgumentException("invalid LinkedMap owner: " + owner);
			if (suffix.length() < 2 || suffix.charAt(0) != '@' || suffix.indexOf('@', 1) >= 0
					|| suffix.substring(1).chars().allMatch(c -> c >= '0' && c <= '9'))
				throw new IllegalArgumentException("invalid LinkedMap listener suffix: " + suffix);
			return _open(owner + suffix, valueClass, nodeSize);
		}

		public <T extends Bean> LinkedMap<T> open(String name, Class<T> valueClass) {
			return open(name, valueClass, 30);
		}

		public <T extends Bean> CHashMap<T> openConcurrent(
				String name, Class<T> valueClass) {
			return openConcurrent(name, valueClass, 30);
		}

		public <T extends Bean> CHashMap<T> openConcurrent(
				String name, Class<T> valueClass, int nodeSize) {
			return openConcurrentWithLayout(name, valueClass, nodeSize);
		}

		/**
		 * 升级前停掉该map的旧写入者，再显式登记历史桶数（旧无nodeSize重载128，旧有nodeSize重载256）。
		 * 不重排业务记录：以后所有普通openConcurrent重载均使用持久登记的布局。
		 * 已混用两个旧布局的数据必须先由业务方核对合并，不能通过本入口自动判断。
		 */
		public void adoptLegacyConcurrentLayout(String name, int bucketCount) {
			validateConcurrentName(name);
			if (bucketCount != 128 && bucketCount != 256)
				throw new IllegalArgumentException("legacy bucketCount must be 128 or 256");
			if (linkedMaps.containsKey(name))
				throw new IllegalStateException("adopt layout before opening '" + name + "'");
			var rc = zeze.newProcedure(() -> {
				var layout = _tLinkedMaps.getOrAdd(concurrentLayoutPrefix + name);
				if (layout.getCount() != 0 && layout.getCount() != bucketCount)
					throw new IllegalStateException("concurrent layout already registered: " + layout.getCount());
				for (var i = bucketCount; i < 256; i++) {
					var root = _tLinkedMaps.get(name + '@' + i);
					if (root != null && (root.getCount() != 0 || root.getHeadNodeId() != 0))
						throw new IllegalStateException("data exists outside requested layout: bucket " + i);
				}
				layout.setCount(bucketCount);
				return 0;
			}, "CHashMap.adoptLegacyLayout").call();
			if (rc != 0)
				throw new IllegalStateException("adoptLegacyConcurrentLayout failed, rc=" + rc);
		}

		private static void validateConcurrentName(String name) {
			if (name.isEmpty() || name.contains("@"))
				throw new IllegalArgumentException("concurrent name must be nonempty and cannot contain '@'");
		}

		@SuppressWarnings("unchecked")
		private <T extends Bean> CHashMap<T> openConcurrentWithLayout(
				String name, Class<T> valueClass, int nodeSize) {
			if (name.isEmpty())
				throw new IllegalArgumentException("name is empty.");
			if (nodeSize < 1)
				throw new IllegalArgumentException("nodeSize < 1");

			validateConcurrentName(name);
			// CHashMap 构造时会嵌套打开分片 LinkedMap，避免在 computeIfAbsent 内递归更新，先 get 再 putIfAbsent。
			var exist = linkedMaps.get(name);
			if (exist instanceof CHashMap)
				return (CHashMap<T>)exist;
			if (null != exist)
				throw new IllegalArgumentException("name '" + name + "' already opened as LinkedMap.");
			var bucketCount = new OutLong();
			var rc = zeze.newProcedure(() -> {
				var layout = _tLinkedMaps.get(concurrentLayoutPrefix + name);
				if (layout == null) {
					for (var i = 0; i < 256; i++) {
						var root = _tLinkedMaps.get(name + '@' + i);
						if (root != null && (root.getCount() != 0 || root.getHeadNodeId() != 0))
							throw new IllegalStateException("legacy concurrent data requires adoptLegacyConcurrentLayout: " + name);
					}
					layout = _tLinkedMaps.getOrAdd(concurrentLayoutPrefix + name);
					layout.setCount(concurrentBuckets);
				}
				if (layout.getCount() != 128 && layout.getCount() != 256)
					throw new IllegalStateException("invalid concurrent layout: " + layout.getCount());
				bucketCount.value = layout.getCount();
				return 0;
			}, "CHashMap.openLayout").call();
			if (rc != 0)
				throw new IllegalStateException("openConcurrent layout failed, rc=" + rc + "; legacy data requires explicit adoption");
			var created = new CHashMap<>(this, name, valueClass, (int)bucketCount.value, nodeSize);
			var prev = linkedMaps.putIfAbsent(name, created);
			if (null != prev) {
				if (prev instanceof CHashMap)
					return (CHashMap<T>)prev;
				throw new IllegalArgumentException("name '" + name + "' already opened as LinkedMap.");
			}
			return created;
		}

		public final ConcurrentHashMap<String, ChangeListener> NodeListeners = new ConcurrentHashMap<>();
		public final ConcurrentHashMap<String, ChangeListener> RootListeners = new ConcurrentHashMap<>();

		private void delayClearJob(DelayRemove delayRemove, String jobId, Binary jobState) {
			var state = new BClearJobState();
			state.decode(ByteBuffer.Wrap(jobState));
			// 空map的clear会提交head=0的job，立即删除job行
			if (state.getHeadNodeId() == 0) {
				zeze.newProcedure(() -> {
					delayRemove.setJobState(jobId, null); // remove job
					return 0;
				}, "LinkedMap.clear").call();
				return;
			}
			while (state.getHeadNodeId() != 0) {
				var result = zeze.newProcedure(() -> {
					// 事务体仅操作表，内存 state 推进放在 whileCommit 以保持 redo 重放幂等。
					var mapName = state.getLinkedMapName();
					var nodeId = state.getHeadNodeId();
					for (var processed = 0; nodeId != 0 && processed < clearJobBatchNodes; processed++) {
						var node = _tLinkedMapNodes.get(new BLinkedMapNodeKey(mapName, nodeId));
						if (null == node) {
							nodeId = 0; // 节点缺失时结束 job，避免死循环
							break;
						}

						// 清理已摘链的旧代节点，只删除仍指向本节点的旧映射，避免误删重建的新映射。
						for (var e : node.getValues()) {
							var key = new BLinkedMapKey(mapName, e.getId());
							var current = _tValueIdToNodeId.get(key);
							if (null != current && current.getNodeId() == nodeId)
								_tValueIdToNodeId.remove(key);
						}
						node.getValues().clear(); // gc
						// clear中的删除节点，马上删除，不需要delayRemove。
						_tLinkedMapNodes.remove(new BLinkedMapNodeKey(mapName, nodeId));
						nodeId = node.getNextNodeId();
					}
					if (nodeId == 0) {
						delayRemove.setJobState(jobId, null); // remove job
					} else {
						// 落库写入推进后的副本，内存 state 在提交后再推进。
						delayRemove.setJobState(jobId, new BClearJobState(nodeId, mapName));
					}
					final var next = nodeId;
					Transaction.whileCommit(() -> state.setHeadNodeId(next));
					return 0;
				}, "LinkedMap.clear").call();
				if (result != 0) {
					// 失败即停，保留上次提交的 job 状态交重启续跑，避免在同一 head 上死循环。
					break;
				}
			}
		}
	}

	private final Module module;
	private final String name;
	private final int nodeSize;
	private final long valueTypeId;

	private LinkedMap(Module module, String name, Class<V> valueClass, int nodeSize) {
		this.module = module;
		this.name = name;
		this.nodeSize = nodeSize;

		beanFactory.register(valueClass);
		this.valueTypeId = BeanFactory.typeId(valueClass);
	}

	public String getName() {
		return name;
	}

	// list：NodeId 仅在当前事务内有效，getNode/getFirstNode 仅供分页读取协议只读使用。
	private @Nullable BLinkedMap getRoot() {
		return module._tLinkedMaps.get(name);
	}

	/**
	 * 按 NodeId 读取节点行，仅供分页读取协议续读使用；返回的活 bean 仅应读取，不可修改。
	 */
	private @Nullable BLinkedMapNode getNodePrivate(long nodeId) {
		return module._tLinkedMapNodes.get(new BLinkedMapNodeKey(name, nodeId));
	}

	public @Nullable BLinkedMapNodeReadOnly getNode(long nodeId) {
		return module._tLinkedMapNodes.get(new BLinkedMapNodeKey(name, nodeId));
	}

	/**
	 * 返回头节点，并把nodeId.value设为头节点Id，供分页读取协议首发使用。map未创建或空链返回null。
	 */
	public @Nullable BLinkedMapNodeReadOnly getFirstNode(OutLong nodeId) {
		var root = getRoot();
		if (null == root)
			return null;
		nodeId.value = root.getHeadNodeId();
		if (root.getHeadNodeId() == 0)
			return null;
		var node = getNode(root.getHeadNodeId());
		if (node == null)
			// 仅记 error 不抛异常：并发删除下瞬时缺失会在提交时因读集冲突回滚重试。
			logger.error("LinkedMap.getFirstNode: head node row missing (broken data?)."
					+ " name={} headNodeId={} count={}", name, root.getHeadNodeId(), root.getCount());
		return node;
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public long size() {
		var root = getRoot();
		return root != null ? root.getCount() : 0;
	}

	public long moveAhead(String id) {
		return move(id, true);
	}

	public long moveTail(String id) {
		return move(id, false);
	}

	private long move(String id, boolean ahead) {
		var nodeId = getValidNodeId(id);
		if (nodeId == null)
			return 0;

		var nodeIdLong = nodeId.getNodeId();
		var node = getNodePrivate(nodeIdLong);
		if (null == node)
			throw new IllegalStateException("NodeId Exist. But Node Not Found. maybe broken data. id=" + id);
		var values = node.getValues();
		if (values.isEmpty())
			throw new IllegalStateException("Node Exist But Values Empty. maybe broken data. id=" + id);

		// activate。优化：这个操作比较多，已经在目标位置，不调整。
		var root = getRoot();
		if (null == root)
			throw new IllegalStateException("root is null. maybe operate before create.");

		if (ahead) {
			if (values.getFirst().getId().equals(id) && root.getHeadNodeId() == nodeIdLong) // HeadNode && List.Last
				return nodeIdLong;
		} else {
			if (values.getLast().getId().equals(id) && root.getTailNodeId() == nodeIdLong) // TailNode && List.Last
				return nodeIdLong;
		}

		for (int i = 0; i < values.size(); i++) {
			var e = values.get(i);
			if (e.getId().equals(id)) {
				values.remove(i);
				if (values.isEmpty())
					removeNodeUnsafe(nodeId.getNodeId(), node);
				// 受管 bean 不可直接跨容器搬移，深拷贝非受管副本后再插入新节点。
				var newNodeValue = e.copy();
				var newNodeId = ahead ? addHeadUnsafe(newNodeValue) : addTailUnsafe(newNodeValue);
				nodeId.setNodeId(newNodeId);
				return newNodeId;
			}
		}
		throw new IllegalStateException("Node Exist But Value Not Found.");
	}

	// map：查索引并同事务读 root 校验代际号，确保并发 clear 触发读集冲突重试。
	private @Nullable BLinkedMapNodeId getValidNodeId(String id) {
		var nodeId = module._tValueIdToNodeId.get(new BLinkedMapKey(name, id));
		if (nodeId == null)
			return null;
		var root = getRoot();
		if (null == root || root.getSerialNo() != nodeId.getSerialNo())
			return null;
		return nodeId;
	}

	@SuppressWarnings("unchecked")
	public V getOrAdd(String id) {
		var value = get(id);
		if (null != value)
			return value;
		value = (V)beanFactory.createBeanFromSpecialTypeId(valueTypeId);
		put(id, value);
		return value;
	}

	public @Nullable V put(long id, V value) {
		return put(String.valueOf(id), value, true);
	}

	public @Nullable V put(String id, V value) {
		return put(id, value, true);
	}

	public @Nullable V put(String id, V value, boolean ahead) {
		var nodeIdKey = new BLinkedMapKey(name, id);
		var mappedNodeId = module._tValueIdToNodeId.get(nodeIdKey);
		var root = getRoot();
		var stale = null != mappedNodeId
				&& (null == root || root.getSerialNo() != mappedNodeId.getSerialNo());
		if (null != mappedNodeId && !stale) {
			var node = getNodePrivate(mappedNodeId.getNodeId());
			if (null == node)
				throw new IllegalStateException("NodeId Exist. But Node Not Found. maybe broken data. id=" + id);
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

		// 无映射或旧代映射时全新插入，不复用已摘链的旧 NodeId。
		var newNodeValue = new BLinkedMapNodeValue();
		newNodeValue.setId(id);
		newNodeValue.getValue().setBean(value);
		var newNodeId = new BLinkedMapNodeId();
		newNodeId.setNodeId(ahead ? addHeadUnsafe(newNodeValue) : addTailUnsafe(newNodeValue));
		root = getRoot(); // addHeadUnsafe内部getOrAdd保证root存在（处理首个put的情况）
		if (null == root)
			throw new IllegalStateException("root is null. maybe operate before create.");
		newNodeId.setSerialNo(root.getSerialNo());
		if (stale)
			mappedNodeId.assign(newNodeId); // 延迟任务尚未删到旧映射行时原地覆写
		else
			module._tValueIdToNodeId.insert(nodeIdKey, newNodeId);
		root.setCount(root.getCount() + 1);
		return null;
	}

	public @Nullable V get(long id) {
		return get(String.valueOf(id));
	}

	public @Nullable V get(String id) {
		var nodeId = getValidNodeId(id);
		if (nodeId == null)
			return null;

		var node = getNodePrivate(nodeId.getNodeId());
		if (null == node)
			throw new IllegalStateException("NodeId Exist. But Node Not Found. maybe broken data. id=" + id);
		for (var e : node.getValues()) {
			if (e.getId().equals(id)) {
				@SuppressWarnings("unchecked")
				var value = (V)e.getValue().getBean();
				return value;
			}
		}
		return null;
	}

	public @Nullable V remove(long id) {
		return remove(String.valueOf(id));
	}

	@SuppressWarnings("unchecked")
	public @Nullable V remove(String id) {
		var nodeKey = new BLinkedMapKey(name, id);
		var nodeId = getValidNodeId(id);
		if (nodeId == null)
			return null;

		var node = getNodePrivate(nodeId.getNodeId());
		if (null == node)
			throw new IllegalStateException("NodeId Exist. But Node Not Found. maybe broken data. id=" + id);
		var values = node.getValues();
		for (int i = 0, n = values.size(); i < n; i++) {
			var e = values.get(i);
			if (e.getId().equals(id)) {
				values.remove(i);
				module._tValueIdToNodeId.remove(nodeKey);
				var root = getRoot();
				if (null == root)
					throw new IllegalStateException("root is null. maybe operate before create.");
				root.setCount(root.getCount() - 1);
				if (values.isEmpty())
					removeNodeUnsafe(nodeId.getNodeId(), node);
				return (V)e.getValue().getBean();
			}
		}
		throw new IllegalStateException("NodeId Exist. But Value Not Found.");
	}

	// foreach
	public void clear() {
		var root = module._tLinkedMaps.get(name);
		if (null != root) {
			var headerNodeId = root.getHeadNodeId();
			// O(1) clear：递增代际号使旧映射整体失效，旧节点由 delayClearJob 异步分批删除；LastNodeId 永不重置以保证 NodeId 唯一。
			root.setSerialNo(root.getSerialNo() + 1);
			root.setHeadNodeId(0);
			root.setTailNodeId(0);
			root.setCount(0);
			module.zeze.getDelayRemove().addJob(Module.eClearJobHandleName, new BClearJobState(headerNodeId, name));
		}
	}

	/**
	 * 遍历全部元素。并发删除导致断链时会从头重启（可能重复回调，count 仅统计最后一轮）；
	 * 重启后仍断在同一节点视为持久断链，抛出 IllegalStateException。
	 */
	@SuppressWarnings("unchecked")
	public long walk(TableWalkHandle<String, V> func) throws Exception {
		long count = 0L;
		long missingNodeId = 0; // 记录上次断链节点，用于识别持久断链
		while (true) {
			var root = module._tLinkedMaps.selectDirty(name);
			if (null == root)
				return func.endWalk(count); // map not created
			var nodeId = root.getHeadNodeId();
			count = 0L;
			while (nodeId != 0) {
				var node = module._tLinkedMapNodes.selectDirty(new BLinkedMapNodeKey(name, nodeId));
				if (null == node) {
					if (missingNodeId == nodeId)
						throw new IllegalStateException("LinkedMap.walk broken chain at " + name + "#" + nodeId);
					missingNodeId = nodeId;
					break; // concurrent node remove, restart walk.
				}
				for (var value : node.getValues()) {
					++count;
					if (!func.handle(value.getId(), (V)value.getValue().getBean()))
						return func.endWalk(count); // user break
				}
				nodeId = node.getNextNodeId();
			}
			if (nodeId == 0)
				return func.endWalk(count); // tail
			// concurrent node remove, restart walk.
		}
	}

	// inner
	private long addHeadUnsafe(BLinkedMapNodeValue nodeValue) {
		var root = module._tLinkedMaps.getOrAdd(name);
		var headNodeId = root.getHeadNodeId();
		var head = headNodeId != 0 ? getNodePrivate(headNodeId) : null;
		if (head == null && headNodeId != 0 && root.getTailNodeId() != 0)
			logger.error("LinkedMap.addHeadUnsafe: head node row missing but chain not empty (broken data?)."
					+ " name={} headNodeId={} tailNodeId={} count={}",
					name, headNodeId, root.getTailNodeId(), root.getCount());
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
		var tail = tailNodeId != 0 ? getNodePrivate(tailNodeId) : null;
		if (tail == null && tailNodeId != 0 && root.getHeadNodeId() != 0)
			logger.error("LinkedMap.addTailUnsafe: tail node row missing but chain not empty (broken data?)."
					+ " name={} tailNodeId={} headNodeId={} count={}",
					name, tailNodeId, root.getHeadNodeId(), root.getCount());
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
		if (null == root)
			throw new IllegalStateException("root is null. maybe operate before create.");

		var prevNodeId = node.getPrevNodeId();
		var nextNodeId = node.getNextNodeId();

		if (prevNodeId == 0) // is head
			root.setHeadNodeId(nextNodeId);
		else {
			var prev = getNodePrivate(prevNodeId);
			if (null == prev)
				throw new IllegalStateException("Prev Node Not Found. maybe broken data. nodeId=" + nodeId);
			prev.setNextNodeId(nextNodeId);
		}

		if (nextNodeId == 0) // is tail
			root.setTailNodeId(prevNodeId);
		else {
			var next = getNodePrivate(nextNodeId);
			if (null == next)
				throw new IllegalStateException("Next Node Not Found. maybe broken data. nodeId=" + nodeId);
			next.setPrevNodeId(prevNodeId);
		}

		// 没有马上删除，启动gc延迟删除。
		module._tLinkedMapNodes.delayRemove(new BLinkedMapNodeKey(name, nodeId));
	}
}
