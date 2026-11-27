package Game.Buf;

import java.util.Objects;
import Zeze.Game.OnlineSpec;
import Zeze.Transaction.*;
import Game.*;
import Zeze.Transaction.Collections.LogMap2;

public class ModuleBuf extends AbstractModule {
	@Override
	public void StartLast() {
		_tbufs.getChangeListenerMap().addListener(new BufChangeListener("Game.Buf.Bufs"));
	}

	public final void Start(App app) {
	}

	public final void Stop(App app) {
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
					notemap2.forEachRemoved(changed2.Argument.getRemove()::add);
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
	public final Bufs getBufs(long roleId) {
		return new Bufs(roleId, _tbufs.getOrAdd(roleId));
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleBuf(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
