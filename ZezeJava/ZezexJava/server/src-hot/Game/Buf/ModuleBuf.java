package Game.Buf;

import org.jspecify.annotations.Nullable;
import java.util.Objects;
import java.util.concurrent.Future;
import Game.Equip.IModuleEquip;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Component.TimerSpec;
import Zeze.Game.OnlineSpec;
import Zeze.Hot.HotService;
import Zeze.Transaction.*;
import Game.*;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModuleBuf extends AbstractModule implements IModuleBuf {
	private static final Logger logger = LogManager.getLogger(ModuleBuf.class);

	private @Nullable Future<?> timerIdHot;
	public int counterHotTimer;

	@Override
	public int getCounter() {
		return counterHotTimer;
	}

	@Override
	public void setCounter(int value) {
		counterHotTimer = value;
	}

	public static class HotTimer implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) throws Exception {
			var mc = Objects.requireNonNull(Objects.requireNonNull(context.timer.zeze).getHotManager()).getModuleContext("Game.Buf", IModuleBuf.class);
			var ibuf = Objects.requireNonNull(mc).getService();
			var buf = (BBuf)context.customData;
			var count = Objects.requireNonNull(ibuf).getCounter();
			if (Objects.requireNonNull(buf).getId() != count)
				throw new RuntimeException("buf " + buf.getId() + ":" + count);
			buf.setId(count + 1);
			Transaction.whileCommit(() -> ibuf.setCounter(count + 1));
		}
	}

	@Override
	public void StartLast() {
		counterHotTimer = 0;
		_tbufs.getChangeListenerMap().addListener(new BufChangeListener("Game.Buf.Bufs"));
		var rand = Zeze.Util.Random.getInstance();
		timerIdHot = TaskSpec.ofAction(
				() -> {
					var module = Objects.requireNonNull(App.getZeze().getHotManager()).getModuleContext("Game.Equip", IModuleEquip.class);
					var service = Objects.requireNonNull(module).getService();
					oldAccess = Objects.requireNonNull(service).hotHelloWorld(oldAccess);
				}).schedulePeriodNow(
				rand.nextLong(3000) + 1000,
				rand.nextLong(3000) + 1000);

		TaskSpec.ofProcedure(App.getZeze().newProcedure(() -> {
			hotTimerId = App.getZeze().getTimer().schedule(
					TimerSpec.ofDelay(rand.nextLong(3000) + 1000).period(rand.nextLong(3000) + 1000),
					HotTimer.class, new BBuf());
			return 0;
		}, "hotTimer")).call();
	}

	int oldAccess = 0;

	public final void Start(App app) {
	}

	@Nullable String hotTimerId;

	@Override
	public void stopBefore() throws Exception {
		StopBefore();
	}

	@Override
	public void StopBefore() throws Exception {
		logger.info("StopBefore {}", this.getFullName());
		if (null != timerIdHot) {
			timerIdHot.cancel(true);
			timerIdHot = null;
		}
		TaskSpec.ofProcedure(App.getZeze().newProcedure(() -> {
			App.getZeze().getTimer().cancel(hotTimerId);
			return 0;
		}, "hotTimer")).call();
	}

	public final void Stop(App app) {
	}

	@Override
	public void start() throws Exception {
		Start(App);
	}

	@Override
	public void startLast() throws Exception {
		StartLast();
	}

	@Override
	public void stop() throws Exception {
		Stop(App);
	}

	@Override
	public void upgrade(HotService old) throws Exception {

	}

	private static class BufChangeListener implements ChangeListener {
		private final String Name;

		public final String getName() {
			return Name;
		}

		public BufChangeListener(String name) {
			Name = name;
		}

		@Override
		public final void OnChanged(Object key, Changes.Record c) {
			switch (c.getState()) {
			case Changes.Record.Put:
				// 记录改变，通知全部。
				BBufs record = (BBufs)c.getValue();

				SChanged changed1 = new SChanged();
				changed1.Argument.setChangeTag(BBufChanged.ChangeTagRecordChanged);
				changed1.Argument.getReplace().putAll(Objects.requireNonNull(record).getBufs());

				OnlineSpec.ofReliableNotify(Objects.requireNonNull(Game.App.Instance.getProvider().getOnline()), (Long)key, getName()).send(changed1);
				break;
			case Changes.Record.Edit:
				// 增量变化，通知变更。
				@SuppressWarnings("unchecked")
				var notemap2 = (LogMap2<Integer, BBuf>)c.getVariableLog(tbufs.VAR_Bufs);
				if (null != notemap2) {
					SChanged changed2 = new SChanged();
					changed2.Argument.setChangeTag(BBufChanged.ChangeTagNormalChanged);
					notemap2.forEachUpsert(changed2.Argument.getReplace()::put);
					for (var p : notemap2.getRemoved()) {
						changed2.Argument.getRemove().add(p);
					}
					OnlineSpec.ofReliableNotify(Objects.requireNonNull(Game.App.getInstance().getProvider().getOnline()), (Long)key, getName()).send(changed2);
				}
				break;
			case Changes.Record.Remove:
				SChanged changed3 = new SChanged();
				changed3.Argument.setChangeTag(BBufChanged.ChangeTagRecordIsRemoved);
				OnlineSpec.ofReliableNotify(Objects.requireNonNull(Game.App.getInstance().getProvider().getOnline()), (Long)key, getName()).send(changed3);
				break;
			}
		}
	}

	// 如果宠物什么的如果也有buf，看情况处理：
	// 统一存到一个表格中（使用BFighetId），或者分开存储。
	// 【建议分开处理】。
	@Override
	public final Bufs getBufs(long roleId) {
		return new Bufs(roleId, _tbufs.getOrAdd(roleId));
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleBuf(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
