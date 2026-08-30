package Zeze.Collections;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import Zeze.Builtin.Collections.LinkedMap.BClearJobState;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMap;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeValue;
import Zeze.Component.DelayRemove;
import Zeze.Hot.HotBeanFactory;
import Zeze.Hot.HotManager;
import Zeze.Hot.HotModule;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.ChangeListener;
import Zeze.Transaction.Changes;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Transaction.Transaction;
import Zeze.Util.ConcurrentHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LinkedMap<V extends Bean> implements HotBeanFactory {
	public static final BeanFactory beanFactory = new BeanFactory();

	public static long getSpecialTypeIdFromBean(@NotNull Serializable bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}

	public static @NotNull Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	private final ConcurrentHashSet<HotModule> hotModulesHaveDynamic = new ConcurrentHashSet<>();
	private final AtomicBoolean freshStopModuleDynamic = new AtomicBoolean();

	private void onHotModuleStop(HotModule hot) {
		if (hotModulesHaveDynamic.remove(hot) != null)
			freshStopModuleDynamic.set(true);
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
	public boolean hasFreshStopModuleDynamicOnce() {
		return freshStopModuleDynamic.getAndSet(false);
	}

	@Override
	public void clearTableCache() {
		module._tLinkedMapNodes.__ClearTableCacheUnsafe__();
	}

	@Override
	public BeanFactory beanFactory() {
		return beanFactory;
	}

	public static class Module extends AbstractLinkedMap {
		private final ConcurrentHashMap<String, Object> linkedMaps = new ConcurrentHashMap<>();
		public final @NotNull Zeze.Application zeze;
		public static final String eClearJobHandleName = "Zeze.Collections.LinkedMap.Clear";
		// delayClearJob每个事务清理的节点数：事务数降为1/K（行删除日志只记key，K个节点的事务仍然小）。
		// K越大，与"clear后立刻按旧id重建"的并发写冲突窗口越大（映射行读写交叉），失败整批回滚重试，自愈。
		public static final int clearJobBatchNodes = 16;

		public Module(@NotNull Zeze.Application zeze) {
			this.zeze = zeze;
			RegisterZezeTables(zeze);

			// 总是监听，但不直接开放。
			// 监听回调按LinkedMap.Name的后缀名进行回调，不支持广播。
			_tLinkedMapNodes.getChangeListenerMap().addListener(this::OnLinkedMapNodeChange);
			_tLinkedMaps.getChangeListenerMap().addListener(this::OnLinkedMapRootChange);

			this.zeze.getDelayRemove().register(eClearJobHandleName, this::delayClearJob);
		}

		public @NotNull ByteBuffer encodeChangeListenerWithSpecialName(@Nullable String specialName,
																	   @NotNull Object key, @NotNull Changes.Record r) {
			return _tLinkedMapNodes.encodeChangeListenerWithSpecialName(specialName, key, r);
		}

		private void OnLinkedMapNodeChange(@NotNull Object key, @NotNull Changes.Record r) {
			var nodeKey = (BLinkedMapNodeKey)key;
			var indexOf = nodeKey.getName().lastIndexOf('@');
			if (indexOf >= 0) {
				var endsWith = nodeKey.getName().substring(indexOf);
				var listener = NodeListeners.get(endsWith);
				if (null != listener)
					listener.OnChanged(key, r);
			}
		}

		private void OnLinkedMapRootChange(@NotNull Object key, @NotNull Changes.Record r) {
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
		<T extends Bean> @NotNull LinkedMap<T> _open(@NotNull String name, @NotNull Class<T> valueClass, int nodeSize) {
			if (nodeSize < 1)
				throw new IllegalArgumentException("nodeSize < 1");
			if (name.isEmpty())
				throw new IllegalArgumentException("name is empty.");

			return (LinkedMap<T>)linkedMaps.computeIfAbsent(name, k -> new LinkedMap<>(this, k, valueClass, nodeSize));
		}

		public <T extends Bean> @NotNull LinkedMap<T> open(@NotNull String name, @NotNull Class<T> valueClass, int nodeSize) {
			if (name.contains("@"))
				throw new IllegalArgumentException("name contains '@', that is reserved.");
			return _open(name, valueClass, nodeSize);
		}

		public <T extends Bean> @NotNull LinkedMap<T> open(@NotNull String name, @NotNull Class<T> valueClass) {
			return open(name, valueClass, 30);
		}

		public <T extends Bean> @NotNull CHashMap<T> openConcurrent(
				@NotNull String name, @NotNull Class<T> valueClass) {
			return openConcurrent(name, valueClass, 128, 30);
		}

		public <T extends Bean> @NotNull CHashMap<T> openConcurrent(
				@NotNull String name, @NotNull Class<T> valueClass, int nodeSize) {
			return openConcurrent(name, valueClass, 256, nodeSize);
		}

		@SuppressWarnings({"unchecked", "SameParameterValue"})
		private <T extends Bean> @NotNull CHashMap<T> openConcurrent(
				@NotNull String name, @NotNull Class<T> valueClass, int concurrencyLevel, int nodeSize) {
			if (name.isEmpty())
				throw new IllegalArgumentException("name is empty.");
			if (nodeSize < 1)
				throw new IllegalArgumentException("nodeSize < 1");

			// concurrencyLevel 应该持久化？因为现在写法，本进程访问会忽略后续不一样的concurrencyLevel，
			//  但是多进程，没有保护到，会出错。
			//  但是如果concurrencyLevel持久化，要不要提供修改它的能力？
			//  题外话：LinkedMap的nodeSize是可以随时改的，它只影响新的node的大小，node大小不一样是可以的。
			//  先不直接暴露这个方法，只暴露固定级别的方法。
			if (name.contains("@"))
				throw new IllegalArgumentException("name contains '@', that is reserved.");
			// CHashMap和LinkedMap共享一个名字空间，并且CHashMap内部还会创建一批LinkedMap。
			// 不能在linkedMaps.computeIfAbsent的mapping function内构造CHashMap：
			// 其构造函数会_open(name@i)对同一个map做嵌套computeIfAbsent，JDK抛Recursive update。
			// 先get，未命中则构造后putIfAbsent竞速（构造幂等：分片_open与initSize都可重入）。
			var exist = linkedMaps.get(name);
			if (exist instanceof CHashMap)
				return (CHashMap<T>)exist;
			if (null != exist)
				throw new IllegalArgumentException("name '" + name + "' already opened as LinkedMap.");
			var created = new CHashMap<>(this, name, valueClass, concurrencyLevel, nodeSize);
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

		private void delayClearJob(@NotNull DelayRemove delayRemove, @NotNull String jobId, @NotNull Binary jobState) {
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
					// lambda只做表操作，不改共享的内存state：perform的redo会重放lambda，
					// 非DB副作用一律走whileCommit（仅在最终提交后执行，redo重放时被丢弃），
					// 重放读到的head不变，天然幂等，不存在跳节点泄漏。
					var mapName = state.getLinkedMapName();
					var nodeId = state.getHeadNodeId();
					for (var processed = 0; nodeId != 0 && processed < clearJobBatchNodes; processed++) {
						var node = _tLinkedMapNodes.get(new BLinkedMapNodeKey(mapName, nodeId));
						if (null == node) {
							nodeId = 0; // 链断（节点不存在）：结束job，否则会无限循环在缺失节点上
							break;
						}

						// 这里是清理已摘链的旧代节点：不调整count（clear已归零）、不动链表指针（已摘链），
						// 只删数据行和仍指向本节点的旧映射——clear后用旧id重建的映射指向新节点，不能误删。
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
					// 链走完删除job行：否则每次clear泄漏一行，重启时continueJobs还会空跑一遍
					if (nodeId == 0) {
						delayRemove.setJobState(jobId, null); // remove job
					} else {
						// job行必须在本事务内记录推进后的head（setJobState是即时序列化），
						// 此时共享state还没推进，落库用副本，内存推进交给whileCommit。
						delayRemove.setJobState(jobId, new BClearJobState(nodeId, mapName));
					}
					final var next = nodeId;
					Transaction.whileCommit(() -> state.setHeadNodeId(next));
					return 0;
				}, "LinkedMap.clear").call();
				if (result != 0) {
					// 失败即停：job行保留最后一次成功提交的状态，等重启continueJobs续跑。
					// 内存state没被碰过（推进只发生在whileCommit），继续循环会在同一head上无限重试。
					break;
				}
			}
		}
	}

	private final @NotNull Module module;
	private final @NotNull String name;
	private final int nodeSize;
	private final long valueTypeId;

	private LinkedMap(@NotNull Module module, @NotNull String name, @NotNull Class<V> valueClass, int nodeSize) {
		var hotManager = module.zeze.getHotManager();
		if (null != hotManager) {
			hotManager.addHotBeanFactory(this);
			tryRecordHotModule(valueClass);
		}

		this.module = module;
		this.name = name;
		this.nodeSize = nodeSize;

		beanFactory.register(valueClass);
		this.valueTypeId = BeanFactory.typeId(valueClass);
	}

	public @NotNull String getName() {
		return name;
	}

	// list
	// NodeId是存储布局的内部细节（move会搬迁、clear会摘链、节点空了会被GC），只在当前事务内有意义，
	// 不作为公共API暴露；外部一律按id操作。getRoot/getNode仅内部使用。
	private @Nullable BLinkedMap getRoot() {
		return module._tLinkedMaps.get(name);
	}

	private BLinkedMapNode getNode(long nodeId) {
		return module._tLinkedMapNodes.get(new BLinkedMapNodeKey(name, nodeId));
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public long size() {
		var root = getRoot();
		return root != null ? root.getCount() : 0;
	}

	public long moveAhead(@NotNull String id) {
		return move(id, true);
	}

	public long moveTail(@NotNull String id) {
		return move(id, false);
	}

	private long move(@NotNull String id, boolean ahead) {
		var nodeId = getValidNodeId(id);
		if (nodeId == null)
			return 0;

		var nodeIdLong = nodeId.getNodeId();
		var node = getNode(nodeIdLong);
		var values = node.getValues();

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
				// 不能直接搬value bean引用：受管bean跨容器移动抛HasManagedException。
				// e.copy()深拷贝得到非受管副本；生成bean的copy()由生成器无条件实现（含DynamicBean内部经setBean保持typeId）。
				var newNodeValue = e.copy();
				var newNodeId = ahead ? addHeadUnsafe(newNodeValue) : addTailUnsafe(newNodeValue);
				nodeId.setNodeId(newNodeId); // 索引必须跟随搬迁指向新节点，否则get/remove找不到、put误判重复
				return newNodeId;
			}
		}
		throw new IllegalStateException("Node Exist But Value Not Found.");
	}

	// map
	// 查索引并验代际：索引缺失或SerialNo与root不相等（clear前旧代映射）都返回null（当作不存在）。
	// 必须在同一事务内读root验章：root进入read-set后与并发clear的提交冲突重试，消灭"事务跨clear窗口"的幽灵读。
	private @Nullable BLinkedMapNodeId getValidNodeId(@NotNull String id) {
		var nodeId = module._tValueIdToNodeId.get(new BLinkedMapKey(name, id));
		if (nodeId == null)
			return null;
		var root = getRoot();
		if (null == root || root.getSerialNo() != nodeId.getSerialNo())
			return null;
		return nodeId;
	}

	@SuppressWarnings("unchecked")
	public @NotNull V getOrAdd(@NotNull String id) {
		var value = get(id);
		if (null != value)
			return value;
		value = (V)beanFactory.createBeanFromSpecialTypeId(valueTypeId);
		put(id, value);
		return value;
	}

	public @Nullable V put(long id, @NotNull V value) {
		return put(String.valueOf(id), value, true);
	}

	public @Nullable V put(@NotNull String id, @NotNull V value) {
		return put(id, value, true);
	}

	public @Nullable V put(@NotNull String id, @NotNull V value, boolean ahead) {
		var nodeIdKey = new BLinkedMapKey(name, id);
		var mappedNodeId = module._tValueIdToNodeId.get(nodeIdKey);
		var root = getRoot();
		var stale = null != mappedNodeId
				&& (null == root || root.getSerialNo() != mappedNodeId.getSerialNo());
		if (null != mappedNodeId && !stale) {
			var node = getNode(mappedNodeId.getNodeId());
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

		// 无映射或clear前旧代映射：当作不存在，全新插入，绝不复用已摘链的旧NodeId。
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
			mappedNodeId.assign(newNodeId); // 旧映射行还在（延迟任务未删到），原地改写；insert会因key已存在抛异常
		else
			module._tValueIdToNodeId.insert(nodeIdKey, newNodeId);
		root.setCount(root.getCount() + 1);
		return null;
	}

	public @Nullable V get(long id) {
		return get(String.valueOf(id));
	}

	public @Nullable V get(@NotNull String id) {
		var nodeId = getValidNodeId(id);
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

	public @Nullable V remove(long id) {
		return remove(String.valueOf(id));
	}

	@SuppressWarnings("unchecked")
	public @Nullable V remove(@NotNull String id) {
		var nodeKey = new BLinkedMapKey(name, id);
		var nodeId = getValidNodeId(id); // 旧代映射当作不存在：不再把count减成负数、不再写已摘链节点
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
			// O(1) clear：递增代际号使全部旧映射整体失效（读侧按SerialNo验章识别旧代、当作不存在，
			// 见getValidNodeId），节点行、bean数据、旧映射由delayClearJob逐节点分批删除。
			// LastNodeId不重置：NodeId永不复用，delayClearJob按NodeId归属判断映射是否可删依赖这一点。
			root.setSerialNo(root.getSerialNo() + 1);
			root.setHeadNodeId(0);
			root.setTailNodeId(0);
			root.setCount(0);
			module.zeze.getDelayRemove().addJob(Module.eClearJobHandleName, new BClearJobState(headerNodeId, name));
		}
	}

	@SuppressWarnings("unchecked")
	public long walk(@NotNull TableWalkHandle<String, V> func) throws Exception {
		long count = 0L;
		var root = module._tLinkedMaps.selectDirty(name);
		if (null == root)
			return func.endWalk(count);

		var nodeId = root.getHeadNodeId();
		while (nodeId != 0) {
			var node = module._tLinkedMapNodes.selectDirty(new BLinkedMapNodeKey(name, nodeId));
			if (null == node)
				return func.endWalk(count); // error
			for (var value : node.getValues()) {
				++count;
				if (!func.handle(value.getId(), (V)value.getValue().getBean()))
					return func.endWalk(count);
			}
			nodeId = node.getNextNodeId();
		}
		return func.endWalk(count);
	}

	// inner
	private long addHeadUnsafe(@NotNull BLinkedMapNodeValue nodeValue) {
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

	private long addTailUnsafe(@NotNull BLinkedMapNodeValue nodeValue) {
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

	private void removeNodeUnsafe(long nodeId, @NotNull BLinkedMapNode node) {
		var root = getRoot();
		if (null == root)
			throw new IllegalStateException("root is null. maybe operate before create.");

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
