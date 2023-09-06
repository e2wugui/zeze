package Game.Buf;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import Game.Equip.IModuleEquip;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Hot.HotService;
import Zeze.Transaction.*;
import Game.*;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public class ModuleBuf extends AbstractModule implements IModuleBuf {
	private static final Logger logger = LogManager.getLogger(ModuleBuf.class);

	private Future<?> timerIdHot;
	public AtomicInteger counterHotTimer;

	@Override
	public AtomicInteger getCounter() {
		return counterHotTimer;
	}

	public static class HotTimer implements TimerHandle {

		@Override
		public void onTimer(@NotNull TimerContext context) throws Exception {
			var mc = context.timer.zeze.getHotManager().getModuleContext("Game.Buf", IModuleBuf.class);
			var ibuf = mc.getService();
			var buf = (BBuf)context.customData;
			if (buf.getId() != ibuf.getCounter().get())
				throw new RuntimeException("buf " + buf.getId() + ":" + ibuf.getCounter().get());
			var id = ibuf.getCounter().incrementAndGet();
			buf.setId(id);
		}

		@Override
		public void onTimerCancel() throws Exception {

		}
	}

	@Override
	public void StartLast() {
		counterHotTimer = new AtomicInteger();
		_tbufs.getChangeListenerMap().addListener(new BufChangeListener("Game.Buf.Bufs"));
		var rand = Zeze.Util.Random.getInstance();
		timerIdHot = Task.scheduleUnsafe(
				rand.nextLong(3000) + 1000,
				rand.nextLong(3000) + 1000,
				() -> {
					var module = App.Zeze.getHotManager().getModuleContext("Game.Equip", IModuleEquip.class);
					var service = module.getService();
					oldAccess = service.hotHelloWorld(oldAccess);
				});

		Task.call(App.Zeze.newProcedure(() -> {
			hotTimerId = App.Zeze.getTimer().schedule(
					rand.nextLong(3000) + 1000, rand.nextLong(3000) + 1000,
					HotTimer.class, new BBuf());
			return 0;
		}, "hotTimer"));
	}

	int oldAccess = 0;
	public final void Start(App app) {
	}

	String hotTimerId;

	@Override
	public void stopBefore() throws Exception {
		StopBefore();
	}

	@Override
	public void StopBefore() throws Exception {
		logger.info("StopBefore " + this.getFullName());
		if (null != timerIdHot) {
			timerIdHot.cancel(true);
			timerIdHot = null;
		}
		Task.call(App.Zeze.newProcedure(() -> {
			App.Zeze.getTimer().cancel(hotTimerId);
			return 0;
		}, "hotTimer"));
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
				changed1.Argument.getReplace().putAll(record.getBufs());

				Game.App.Instance.getProvider().getOnline().sendReliableNotify((Long)key, getName(), changed1);
				break;
			case Changes.Record.Edit:
				// 增量变化，通知变更。
				@SuppressWarnings("unchecked")
				var notemap2 = (LogMap2<Integer, BBuf>)c.getVariableLog(tbufs.VAR_Bufs);
				if (null != notemap2) {
					notemap2.mergeChangedToReplaced();
					SChanged changed2 = new SChanged();
					changed2.Argument.setChangeTag(BBufChanged.ChangeTagNormalChanged);
					changed2.Argument.getReplace().putAll(notemap2.getReplaced());
					for (var p : notemap2.getRemoved()) {
						changed2.Argument.getRemove().add(p);
					}
					Game.App.getInstance().getProvider().getOnline().sendReliableNotify((Long)key, getName(), changed2);
				}
				break;
			case Changes.Record.Remove:
				SChanged changed3 = new SChanged();
				changed3.Argument.setChangeTag(BBufChanged.ChangeTagRecordIsRemoved);
				Game.App.getInstance().getProvider().getOnline().sendReliableNotify((Long)key, getName(), changed3);
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

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
    public ModuleBuf(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
