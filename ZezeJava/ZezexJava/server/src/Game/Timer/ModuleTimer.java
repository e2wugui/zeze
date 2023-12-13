package Game.Timer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Game.App;
import Game.LongSet.ModuleLongSet;
import Game.LongSet.NameValue;
import Zeze.Arch.RedirectToServer;
import Zeze.Component.AutoKey;
import Zeze.Hot.HotService;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Transaction;
import Zeze.Util.Action2;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutLong;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModuleTimer extends AbstractModule implements IModuleTimer {
	private static final Logger logger = LogManager.getLogger(ModuleTimer.class);

	public static final int TimerCountPerNode = 200;

	private AutoKey NodeIdGenerator;
	private AutoKey TimerIdGenerator;

	public void Start(Game.App app) throws Exception {
		NodeIdGenerator = app.Zeze.getAutoKey("Game.Timer.NodeIdGenerator");
		TimerIdGenerator = app.Zeze.getAutoKey("Game.Timer.TimerIdGenerator");
		Task.run(this::LoadTimerLocal, "LoadTimerLocal", DispatchMode.Normal);
	}

	public void Stop(Game.App app) throws Exception {
	}

	// 保存所有可用的timer处理回调，由于可能需要把timer的触发派发到其他服务器执行，必须静态注册。
	// 一般在Module.Initialize中注册即可。
	private final ConcurrentHashMap<String, Action2<Long, String>> TimerHandles = new ConcurrentHashMap<>();

	@Override
	public void cancel(long timerId) {
		var index = _tIndexs.get(timerId);
		if (index == null) {
			// 尽可能的执行取消操作，不做严格判断。
			CancelTimerLocal(App.Zeze.getConfig().getServerId(), timerId, 0, null);
			return;
		}

		// 由于现在ModuleRedirect的实现没有实现loop-back功能，所以这里单独检查。
		if (index.getServerId() == App.Zeze.getConfig().getServerId()) {
			CancelTimerLocal(App.Zeze.getConfig().getServerId(), timerId, index.getNodeId(), _tNodes.get(index.getNodeId()));
		} else {
			Cancel(index.getServerId(), timerId);
		}
	}

	@RedirectToServer
	protected void Cancel(int serverId, long timerId) {
		// 尽可能的执行取消操作，不做严格判断。
		var index = _tIndexs.get(timerId);
		if (index == null) {
			CancelTimerLocal(serverId, timerId, 0, null);
			return;
		}
		if (index.getServerId() != App.Zeze.getConfig().getServerId())
			logger.error("Cancel@RedirectToServer Not Local.");
		CancelTimerLocal(serverId, timerId, index.getNodeId(), _tNodes.get(index.getNodeId()));
	}

	private final LongConcurrentHashMap<Future<?>> TimersLocal = new LongConcurrentHashMap<>();

	private void CancelTimerLocal(int serverId, long nodeId, long timerId, BNode node) {

		var local = TimersLocal.remove(timerId);
		if (local != null)
			local.cancel(false);

		if (node == null)
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
			_tNodes.delayRemove(nodeId);
		}
	}

	@Override
	public long schedule(long delay, long period, long times, String name) {
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
				var timer = new BTimer();
				timer.setTimerId(timerId);
				timer.setName(name);
				timer.setDelay(delay);
				timer.setPeriod(period);
				timer.setRemainTimes(period < 0 ? 1 : times);
				node.getTimers().put(timerId, timer);

				var index = new BIndex();
				index.setServerId(serverId);
				index.setNodeId(nodeId);
				_tIndexs.tryAdd(timerId, index);

				final var finalNodeId = nodeId;
				Transaction.whileCommit(() -> ScheduleLocal(serverId, timerId, finalNodeId, delay, period, name));
				return timerId;
			}
			nodeId = NodeIdGenerator.nextId();
		}
	}

	private void ScheduleLocal(int serverId, long timerId, long nodeId, long delay, long period, String name) {
		if (period > 0) {
			TimersLocal.put(timerId, Task.scheduleUnsafe(delay, period, () -> TriggerTimerLocal(serverId, timerId, nodeId, name)));
		} else {
			TimersLocal.put(timerId, Task.scheduleUnsafe(delay, () -> TriggerTimerLocal(serverId, timerId, nodeId, name)));
		}
	}

	private long TriggerTimerLocal(int serverId, long timerId, long nodeId, String name) {
		var handle = TimerHandles.get(name);
		if (handle != null) {
			Task.call(App.Zeze.newProcedure(() -> {
				handle.run(timerId, name);
				return 0L;
			}, "TriggerTimerLocal"));
		}
		Task.call(App.Zeze.newProcedure(() -> {
			var index = _tIndexs.get(timerId);
			if (index != null) {
				var node = _tNodes.get(index.getNodeId());
				if (node != null) {
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
		final var out = new OutObject<BNodeRoot>();
		Task.call(App.Zeze.newProcedure(() ->
		{
			var root = _tNodeRoot.getOrAdd(serverId);
			// 本地每次load都递增。用来处理和接管的并发。
			root.setLoadSerialNo(root.getLoadSerialNo() + 1);
			out.value = root.copy();
			return 0L;
		}, "LoadTimerLocal"));
		var root = out.value;

		return LoadTimerLocal(root.getHeadNodeId(), root.getHeadNodeId(), serverId);
	}

	// 当ServiceManager发现某台GameServer宕机，
	// 它应该随机选择一台可用的GameServer把原来的Timer接管过来。
	// 收到接管通知的服务器调用这个函数进行接管处理。
	// @serverId 需要接管的服务器Id。
	@SuppressWarnings("unused")
	private long SpliceAndLoadTimerLocal(int serverId, long loadSerialNo) {
		if (serverId == App.Zeze.getConfig().getServerId())
			throw new IllegalArgumentException();

		final var first = new OutLong();
		final var last = new OutLong();

		var result = Task.call(App.Zeze.newProcedure(() ->
		{
			var src = _tNodeRoot.get(serverId);
			if (src == null || src.getHeadNodeId() == 0 || src.getTailNodeId() == 0)
				return 0L; // nothing need to do.

			if (src.getLoadSerialNo() != loadSerialNo)
				return 0L; // 需要接管的机器已经活过来了或者已被别人接管。

			// prepare splice
			var root = _tNodeRoot.getOrAdd(App.Zeze.getConfig().getServerId());
			var srcHead = _tNodes.get(src.getHeadNodeId());
			var srcTail = _tNodes.get(src.getTailNodeId());
			var head = _tNodes.get(root.getHeadNodeId());
			//var tail = _tNodes.get(root.getTailNodeId());

			// 先保存存储过程退出以后需要装载的timer范围。
			first.value = src.getHeadNodeId();
			last.value = root.getHeadNodeId();
			// splice
			srcTail.setNextNodeId(root.getHeadNodeId());
			root.setHeadNodeId(src.getHeadNodeId());
			head.setPrevNodeId(src.getTailNodeId());
			srcHead.setPrevNodeId(root.getTailNodeId());
			// clear src
			src.setHeadNodeId(0L);
			src.setTailNodeId(0L);
			return 0L;
		}, "SpliceAndLoadTimerLocal"));

		if (result == 0L) {
			return LoadTimerLocal(first.value, last.value, serverId);
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
					Task.call(App.Zeze.newProcedure(() -> {
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
    public ModuleTimer(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
