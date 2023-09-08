package Game.LongSet;

import java.util.Map;
import java.util.function.Predicate;
import Game.App;
import Game.Timer.ModuleTimer;
import Zeze.Component.AutoKey;
import Zeze.Hot.HotService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModuleLongSet extends AbstractModule implements IModuleLongSet {
	private static final Logger logger = LogManager.getLogger(ModuleLongSet.class);

	public static final int CountPerNode = 500;
	private AutoKey NodeIdGenerator;

	public void Start(Game.App app) throws Exception {
		NodeIdGenerator = app.Zeze.getAutoKey("Game.LongSet.NodeIdGenerator");
	}

	public void Stop(Game.App app) throws Exception {
	}

	@Override
	public boolean add(String name, NameValue value) {
		var root = _tNodeRoot.getOrAdd(name);
		var nodeId = root.getHeadNodeId();
		if (nodeId == 0) { // no node
			nodeId = NodeIdGenerator.nextId();
		}
		var indexKey = name + "#" + value.getName() + "=" + value.getValue();
		var index = _tIndexs.get(indexKey);
		if (index != null)
			return false;

		while (true) {
			var node = _tNodes.getOrAdd(nodeId);
			// 如果节点是新创建的，这里根据node的变量来判断。
			if (node.getNextNodeId() == 0 || node.getPrevNodeId() == 0) {

				if (root.getHeadNodeId() == 0 || root.getTailNodeId() == 0) {
					// root is empty
					node.setPrevNodeId(nodeId);
					node.setNextNodeId(nodeId);
					root.setHeadNodeId(nodeId);
					root.setTailNodeId(nodeId);
				} else {
					// link to root head
					var head = _tNodes.get(root.getHeadNodeId());
					head.setPrevNodeId(nodeId);
					node.setNextNodeId(root.getHeadNodeId());
					node.setPrevNodeId(root.getTailNodeId());
					root.setHeadNodeId(nodeId);
				}
			}

			if (node.getSet().size() < CountPerNode) {
				var indexNodeId = new BNodeId();
				indexNodeId.setNodeId(nodeId);
				return _tIndexs.tryAdd(indexKey, indexNodeId)
						&& null == node.getSet().putIfAbsent(value, new Timestamp(System.currentTimeMillis()));
			}
			nodeId = NodeIdGenerator.nextId();
		}
	}

	@Override
	public boolean remove(String name, NameValue value) {
		var indexKey = name + "#" + value.getName() + "=" + value.getValue();
		var index = _tIndexs.get(indexKey);
		if (index == null)
			return false;
		_tIndexs.remove(indexKey); // remove always.

		var node = _tNodes.get(index.getNodeId());
		if (node == null)
			return false;

		var result = node.getSet().remove(value) != null;
		if (node.getSet().isEmpty()) {
			var prev = _tNodes.get(node.getPrevNodeId());
			var next = _tNodes.get(node.getNextNodeId());

			var root = _tNodeRoot.get(name);
			if (root.getHeadNodeId() == root.getTailNodeId()) {
				// only one node and will be removed.
				root.setHeadNodeId(0L);
				root.setTailNodeId(0L);
			} else {
				if (root.getHeadNodeId() == index.getNodeId())
					root.setHeadNodeId(node.getNextNodeId());
				if (root.getTailNodeId() == index.getNodeId())
					root.setTailNodeId(node.getPrevNodeId());
			}

			prev.setNextNodeId(node.getNextNodeId());
			next.setPrevNodeId(node.getPrevNodeId());

			// 把当前空的Node加入垃圾回收。
			// 由于Nodes并发访问的原因，不能马上删除。延迟一定时间就安全了。
			// 不删除的话就会在数据库留下垃圾。
			_tNodes.delayRemove(index.getNodeId());
		}
		return result;
	}

	@Override
	public void clear(String name) {
		try {
			foreach(name, (e) -> remove(name, e.getKey()));
		} catch (Throwable ex) {
			// print stacktrace.
			logger.error("_clear", ex);
		}
	}

	@Override
	public void foreach(String name, Predicate<Map.Entry<NameValue, Timestamp>> func1) {
		var root = _tNodeRoot.get(name);
		if (null == root)
			return;

		foreach(root.getHeadNodeId(), root.getHeadNodeId(), func1);
	}

	@Override
	public void foreach(long first, long last, Predicate<Map.Entry<NameValue, Timestamp>> func1) {
		while (true) {
			var node = _tNodes.selectDirty(first);
			if (node == null)
				break; // when root is empty。no node。

			for (var e : node.getSet().entrySet()) {
				final var breakNow = new Zeze.Util.OutObject<Boolean>();
				breakNow.value = false;

				if (0L != Zeze.Util.Task.call(App.Zeze.newProcedure(() -> {
					breakNow.value = func1.test(e);
					return 0L;
				}, "_foreach.callback")))
					break;

				if (breakNow.value)
					break;
			}

			first = node.getNextNodeId();
			if (first == last)
				break;
		}
	}
	@Override
	public void start() throws Exception {
		Start(App);
	}

	@Override
	public void stop() throws Exception {
		Stop(App);
	}

	@Override
	public void upgrade(HotService old) throws Exception {

	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLongSet(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
