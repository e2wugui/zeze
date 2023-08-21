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
import org.jetbrains.annotations.NotNull;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public class ModuleBuf extends AbstractModule implements IModuleBuf {
	private Future<?> timerIdHot;

	public static class HotTimer implements TimerHandle {
		public static AtomicInteger counter = new AtomicInteger();

		@Override
		public void onTimer(@NotNull TimerContext context) throws Exception {
			System.out.println("HotTimer " + counter.incrementAndGet());
		}

		@Override
		public void onTimerCancel() throws Exception {

		}
	}

	int oldAccess = 0;
	public final void Start(App app) {
		_tbufs.getChangeListenerMap().addListener(new BufChangeListener("Game.Buf.Bufs"));
		timerIdHot = Task.scheduleUnsafe(2000, 2000, () -> {
			var module = app.Zeze.getHotManager().getModuleContext("Game.Equip", IModuleEquip.class);
			var service = module.getService();
			oldAccess = service.hotHelloWorld(oldAccess);
			System.out.println("oldAccess=" + oldAccess);
		});

		Task.call(app.Zeze.newProcedure(() -> {
			hotTimerId = app.Zeze.getTimer().schedule(2000, 2000, HotTimer.class, null);
			return 0;
		}, "hotTimer"));
	}

	String hotTimerId;

	public final void Stop(App app) {
		Task.call(app.Zeze.newProcedure(() -> {
			app.Zeze.getTimer().cancel(hotTimerId);
			return 0;
		}, "hotTimer"));

		if (null != timerIdHot)
			timerIdHot.cancel(true);
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
