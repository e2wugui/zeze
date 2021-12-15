package Game.LongSet;

// ZEZE_FILE_CHUNK {{{ IMPORT GEN
// ZEZE_FILE_CHUNK }}} IMPORT GEN

import Game.AutoKey.ModuleAutoKey;
import com.mysql.cj.result.ZeroDateTimeToDefaultValueFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModuleLongSet extends AbstractModule {
    private static final Logger logger = LogManager.getLogger(ModuleLongSet.class);

    public final static int CountPerNode = 500;
    private ModuleAutoKey.AutoKey NodeIdGenerator;

    public void Start(Game.App app) throws Throwable {
        NodeIdGenerator = Game.AutoKey.ModuleAutoKey.getAutoKey("Game.LongSet.NodeIdGenerator");
    }

    public void Stop(Game.App app) throws Throwable {
    }

    public static boolean add(String name, long value) {
        return Game.App.Instance.Game_LongSet._add(name, value);
    }

    public static boolean remove(String name, long value) {
        return Game.App.Instance.Game_LongSet._remove(name, value);
    }

    public static void clear(String name) {
        Game.App.Instance.Game_LongSet._clear(name);
    }

    public static void foreach(String name, Zeze.Util.Func1<Long, Boolean> func1) throws Throwable {
        Game.App.Instance.Game_LongSet._foreach(name, func1);
    }

    private boolean _add(String name, long value) {
        var root = _tNodeRoot.getOrAdd(name);
        var nodeId = root.getHeadNodeId();
        if (nodeId == 0) { // no node
            nodeId = NodeIdGenerator.nextId();
        }
        var indexKey = name + "#" + value;
        var index = _tIndexs.get(indexKey);
        if (null != index)
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

            if (node.getLongSet().size() < CountPerNode) {
                var indexNodeId = new NodeId();
                indexNodeId.setNodeId(nodeId);
                return _tIndexs.tryAdd(indexKey, indexNodeId) && node.getLongSet().add(value);
            }
            nodeId = NodeIdGenerator.nextId();
        }
    }

    private boolean _remove(String name, long value) {
        var indexKey = name + "#" + value;
        var index = _tIndexs.get(indexKey);
        if (null == index)
            return false;
        _tIndexs.remove(indexKey); // remove always.

        var node = _tNodes.get(index.getNodeId());
        if (null == node)
            return false;

        var result = node.getLongSet().remove(value);
        if (node.getLongSet().isEmpty()) {
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

            // TODO 把当前空的Node加入垃圾回收。
            // 由于Nodes并发访问的原因，不能马上删除。延迟一定时间就安全了。
            // 不删除的话就会在数据库留下垃圾。
        }
        return result;
    }

    private void _clear(String name) {
        try {
            _foreach(name, (value) -> _remove(name, value));
        } catch (Throwable ex) {
            logger.error("_clear", ex);
        }
    }

    private void _foreach(String name, Zeze.Util.Func1<Long, Boolean> func1) throws Throwable {
        var root = _tNodeRoot.get(name);
        if (null == root)
            return;

        _foreach(root.getHeadNodeId(), root.getHeadNodeId(), func1);
    }

    private void _foreach(long first, long last, Zeze.Util.Func1<Long, Boolean> func1) throws Throwable {
        while (true) {
            var node = _tNodes.selectDirty(first);
            if (null == node)
                break; // when root is empty。no node。

            for (var value : node.getLongSet()) {
                final var breakNow = new Zeze.Util.OutObject<Boolean>();
                breakNow.Value = false;

                if (0L != Zeze.Util.Task.Call(App.Zeze.NewProcedure(() ->
                        {
                            breakNow.Value = func1.call(value);
                            return 0L;
                        }, "_foreach.callback")))
                    break;

                if (breakNow.Value)
                    break;
            }

            first = node.getNextNodeId();
            if (first == last)
                break;
        }
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE
    public static final int ModuleId = 12;

    private tIndexs _tIndexs = new tIndexs();
    private tNodeRoot _tNodeRoot = new tNodeRoot();
    private tNodes _tNodes = new tNodes();

    public Game.App App;

    public ModuleLongSet(Game.App app) {
        App = app;
        // register protocol factory and handles
        var _reflect = new Zeze.Util.Reflect(this.getClass());
        // register table
        App.Zeze.AddTable(App.Zeze.getConfig().GetTableConf(_tIndexs.getName()).getDatabaseName(), _tIndexs);
        App.Zeze.AddTable(App.Zeze.getConfig().GetTableConf(_tNodeRoot.getName()).getDatabaseName(), _tNodeRoot);
        App.Zeze.AddTable(App.Zeze.getConfig().GetTableConf(_tNodes.getName()).getDatabaseName(), _tNodes);
    }

    public void UnRegister() {
        App.Zeze.RemoveTable(App.Zeze.getConfig().GetTableConf(_tIndexs.getName()).getDatabaseName(), _tIndexs);
        App.Zeze.RemoveTable(App.Zeze.getConfig().GetTableConf(_tNodeRoot.getName()).getDatabaseName(), _tNodeRoot);
        App.Zeze.RemoveTable(App.Zeze.getConfig().GetTableConf(_tNodes.getName()).getDatabaseName(), _tNodes);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE

}
