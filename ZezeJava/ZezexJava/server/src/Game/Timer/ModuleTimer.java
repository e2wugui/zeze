package Game.Timer;

// ZEZE_FILE_CHUNK {{{ IMPORT GEN
// ZEZE_FILE_CHUNK }}} IMPORT GEN

import Game.AutoKey.ModuleAutoKey;
import Game.LongSet.NameValue;
import Zeze.Transaction.Transaction;
import Zeze.Util.OutObject;
import Zezex.RedirectToServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import Game.LongSet.ModuleLongSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class ModuleTimer extends AbstractModule {
    // TODO 需要一个全局Timer（所有服务器只有一个）执行逻辑。
    public final static String GC_LinkedNodesName = "Zezex.GcLinkedNodesName";

    private static final Logger logger = LogManager.getLogger(ModuleTimer.class);

    public static final int TimerCountPerNode = 200;

    private ModuleAutoKey.AutoKey NodeIdGenerator;
    private ModuleAutoKey.AutoKey TimerIdGenerator;

    public void Start(Game.App app) throws Throwable {
        NodeIdGenerator = Game.AutoKey.ModuleAutoKey.getAutoKey("Game.Timer.NodeIdGenerator");
        TimerIdGenerator = Game.AutoKey.ModuleAutoKey.getAutoKey("Game.Timer.TimerIdGenerator");
        Zeze.Util.Task.run(this::LoadTimerLocal, "LoadTimerLocal");
    }

    public void Stop(Game.App app) throws Throwable {
    }

    // 保存所有可用的timer处理回调，由于可能需要把timer的触发派发到其他服务器执行，必须静态注册。
    // 一般在Module.Initialize中注册即可。
    private ConcurrentHashMap<String, Zeze.Util.Action2<Long, String>> TimerHandles = new ConcurrentHashMap<>();

    public static void removeTimerHandle(String name) {
        Game.App.Instance.Game_Timer.TimerHandles.remove(name);
    }

    public static void addTimerHandle(String name, Zeze.Util.Action2<Long, String> action) {
        if (null != Game.App.Instance.Game_Timer.TimerHandles.putIfAbsent(name, action))
            throw new RuntimeException("duplicate timer handle name of: " + name);
    }

    // 取消一个具体的Timer实例。
    public static void cancel(long timerId) {
        Game.App.Instance.Game_Timer._cancel(timerId);
    }

    // 调度一个Timer实例。
    // name为静态注册到这个模块的处理名字。
    // 相同的name可以调度多个timer实例。
    // @return 返回 TimerId。
    public static long schedule(long delay, long period, String name) {
        return Game.App.Instance.Game_Timer._schedule(delay, period, -1, name);
    }

    public static long schedule(long delay, long period, long times, String name) {
        return Game.App.Instance.Game_Timer._schedule(delay, period, times, name);
    }

    // 调度一个Timeout，即仅执行一次的Timer。
    public static long schedule(long delay, String name) {
        return Game.App.Instance.Game_Timer._schedule(delay, -1, 1, name);
    }

    public void _cancel(long timerId) {
        var index = _tIndexs.get(timerId);
        if (null == index) {
            // 尽可能的执行取消操作，不做严格判断。
            CancelTimerLocal(App.Zeze.getConfig().getServerId(), timerId, 0,null);
            return;
        }

        // 由于现在ModuleRedirect的实现没有实现loop-back功能，所以这里单独检查。
        if (index.getServerId() == App.Zeze.getConfig().getServerId()) {
            CancelTimerLocal(App.Zeze.getConfig().getServerId(), timerId, index.getNodeId(), _tNodes.get(index.getNodeId()));
        } else {
            RunCancel(index.getServerId(), timerId);
        }
    }

    @RedirectToServer
    protected void RunCancel(int serverId, long timerId) {
        Cancel(serverId, timerId);
    }

    protected void Cancel(int serverId, long timerId) {
        // 尽可能的执行取消操作，不做严格判断。
        var index = _tIndexs.get(timerId);
        if (null == index) {
            CancelTimerLocal(serverId, timerId, 0,null);
            return;
        }
        if (index.getServerId() != App.Zeze.getConfig().getServerId())
            logger.error("Cancel@RedirectToServer Not Local.");
        CancelTimerLocal(serverId, timerId, index.getNodeId(), _tNodes.get(index.getNodeId()));
    }

    private final ConcurrentHashMap<Long, Future<?>> TimersLocal = new ConcurrentHashMap<>();

    private void CancelTimerLocal(int serverId, long nodeId, long timerId, Node node) {

        var local = TimersLocal.remove(timerId);
        if (null != local)
            local.cancel(false);

        if (null == node)
            return;

        var timers = node.getTimers();
        timers.remove(timerId);

        if (timers.isEmpty()) {
            var prev = _tNodes.get(node.getPrevNodeId());
            var next = _tNodes.get(node.getNextNodeId());
            var root = _tNodeRoot.get(serverId);
            if (root.getHeadNodeId() == root.getTailNodeId()) {
                // only one node and will be removed.
                root.setHeadNodeId(0L);
                root.setTailNodeId(0L);
            } else {
                if (root.getHeadNodeId() == nodeId)
                    root.setHeadNodeId(node.getNextNodeId());
                if (root.getTailNodeId() == nodeId)
                    root.setTailNodeId(node.getPrevNodeId());
            }
            prev.setNextNodeId(node.getNextNodeId());
            next.setPrevNodeId(node.getPrevNodeId());

            // 把当前空的Node加入垃圾回收。
            // 由于Nodes并发访问的原因，不能马上删除。延迟一定时间就安全了。
            // 不删除的话就会在数据库留下垃圾。
            ModuleLongSet.add(GC_LinkedNodesName, new NameValue(_tNodes.getName(), nodeId));
        }
    }

    public long _schedule(long delay, long period, long times, String name) {
        if (delay < 0)
            throw new IllegalArgumentException();

        var serverId = App.Zeze.getConfig().getServerId();
        var root = _tNodeRoot.getOrAdd(serverId);
        var nodeId = root.getHeadNodeId();
        if (nodeId == 0) { // no node
            nodeId = NodeIdGenerator.nextId();
        }
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

            if (node.getTimers().size() < TimerCountPerNode) {
                var timerId = TimerIdGenerator.nextId();
                var timer = new Timer();
                timer.setTimerId(timerId);
                timer.setName(name);
                timer.setDelay(delay);
                timer.setPeriod(period);
                timer.setRemainTimes(period < 0 ? 1 : times);
                node.getTimers().put(timerId, timer);

                var index = new Index();
                index.setServerId(serverId);
                index.setNodeId(nodeId);
                _tIndexs.tryAdd(timerId, index);

                final var finalNodeId = nodeId;
                Transaction.getCurrent().RunWhileCommit(()-> ScheduleLocal(serverId, timerId, finalNodeId, delay, period, name));
                return timerId;
            }
            nodeId = NodeIdGenerator.nextId();
        }
    }

    private void ScheduleLocal(int serverId, long timerId, long nodeId, long delay, long period, String name) {
        if (period > 0) {
            TimersLocal.put(timerId, Zeze.Util.Task.schedule(delay, period, () -> TriggerTimerLocal(serverId, timerId, nodeId, name)));
        } else {
            TimersLocal.put(timerId, Zeze.Util.Task.schedule(delay, () -> TriggerTimerLocal(serverId, timerId, nodeId, name)));
        }
    }

    private long TriggerTimerLocal(int serverId, long timerId, long nodeId, String name) {
        final var handle = TimerHandles.get(name);
        if (null != handle) {
            Zeze.Util.Task.Call(App.Zeze.NewProcedure(()-> {
                handle.run(timerId, name);
                return 0L;
            }, "TriggerTimerLocal"));
        }
        Zeze.Util.Task.Call(App.Zeze.NewProcedure(()-> {
            var index = _tIndexs.get(timerId);
            if (null != index) {
                var node = _tNodes.get(index.getNodeId());
                if (null != node) {
                    if (handle == null) {
                        CancelTimerLocal(serverId, timerId, nodeId, node);
                    } else {
                        var timer = node.getTimers().get(timerId);
                        if (timer.getRemainTimes() > 0) {
                            timer.setRemainTimes(timer.getRemainTimes() - 1);
                            if (timer.getRemainTimes() == 0) {
                                CancelTimerLocal(serverId, timerId, nodeId, node);
                            }
                        }
                    }
                }
            }
            return 0L;
        }, "AfterTriggerTimerLocal"));
        return 0L;
    }

    private long LoadTimerLocal() {
        var serverId = App.Zeze.getConfig().getServerId();
        final var out = new OutObject<NodeRoot>();
        Zeze.Util.Task.Call(App.Zeze.NewProcedure(() ->
        {
            var root = _tNodeRoot.getOrAdd(serverId);
            // 本地每次load都递增。用来处理和接管的并发。
            root.setLoadSerialNo(root.getLoadSerialNo() + 1);
            out.Value = root.Copy();
            return 0L;
        }, "LoadTimerLocal"));
        var root = out.Value;

        return LoadTimerLocal(root.getHeadNodeId(), root.getHeadNodeId(), serverId);
    }

    // TODO 当ServiceManager发现某台GameServer宕机，
    // TODO 它应该随机选择一台可用的GameServer把原来的Timer接管过来。
    // 收到接管通知的服务器调用这个函数进行接管处理。
    // @serverId 需要接管的服务器Id。
    private long SpliceAndLoadTimerLocal(int serverId, long loadSerialNo) {
        if (serverId == App.Zeze.getConfig().getServerId())
            throw new IllegalArgumentException();

        final var first = new OutObject<Long>();
        final var last = new OutObject<Long>();

        var result = Zeze.Util.Task.Call(App.Zeze.NewProcedure(() ->
        {
            var src = _tNodeRoot.get(serverId);
            if (null == src || src.getHeadNodeId() == 0 || src.getTailNodeId() == 0)
                return 0L; // nothing need to do.

            if (src.getLoadSerialNo() != loadSerialNo)
                return 0L; // 需要接管的机器已经活过来了。

            // prepare splice
            var root = _tNodeRoot.getOrAdd(App.Zeze.getConfig().getServerId());
            var srchead = _tNodes.get(src.getHeadNodeId());
            var srctail = _tNodes.get(src.getTailNodeId());
            var head = _tNodes.get(root.getHeadNodeId());
            //var tail = _tNodes.get(root.getTailNodeId());

            // 先保存存储过程退出以后需要装载的timer范围。
            first.Value = src.getHeadNodeId();
            last.Value = root.getHeadNodeId();
            // splice
            srctail.setNextNodeId(root.getHeadNodeId());
            root.setHeadNodeId(src.getHeadNodeId());
            head.setPrevNodeId(src.getTailNodeId());
            srchead.setPrevNodeId(root.getTailNodeId());
            // clear src
            src.setHeadNodeId(0L);
            src.setTailNodeId(0L);
            return 0L;
        }, "SpliceAndLoadTimerLocal"));

        if (0L == result) {
            return LoadTimerLocal(first.Value, last.Value, serverId);
        }
        return result;
    }

    // 如果存在node，至少执行一次循环。
    private long LoadTimerLocal(long first, long last, int serverId) {
        while (true) {
            var node = _tNodes.selectDirty(first);
            if (null == node)
                break; // when root is empty。no node。

            for (var timer : node.getTimers().values()) {
                ScheduleLocal(serverId, timer.getTimerId(), first, timer.getDelay(), timer.getPeriod(), timer.getName());
                if (serverId != App.Zeze.getConfig().getServerId()) {
                    Zeze.Util.Task.Call(App.Zeze.NewProcedure(() -> {
                        var index = _tIndexs.get(timer.getTimerId());
                        index.setServerId(serverId);
                        return 0L;
                    }, "SetTimerServerIdWhenLoadTimerLocal"));
                }
            }

            first = node.getNextNodeId();
            if (first == last)
                break;
        }
        return 0L;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE
    public ModuleTimer(Game.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE
}
