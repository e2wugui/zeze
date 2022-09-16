package Game.Buf;

import Zeze.Transaction.*;
import Game.*;
import Zeze.Transaction.Collections.LogMap2;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public class ModuleBuf extends AbstractModule {
	public final void Start(App app) {
		_tbufs.getChangeListenerMap().addListener(new BufChangeListener("Game.Buf.Bufs"));
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
				changed1.Argument.getReplace().putAll(record.getBufs());

				Game.App.Instance.getProvider().Online.sendReliableNotify((Long)key, getName(), changed1);
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
					Game.App.getInstance().getProvider().Online.sendReliableNotify((Long)key, getName(), changed2);
				}
				break;
			case Changes.Record.Remove:
				SChanged changed3 = new SChanged();
				changed3.Argument.setChangeTag(BBufChanged.ChangeTagRecordIsRemoved);
				Game.App.getInstance().getProvider().Online.sendReliableNotify((Long)key, getName(), changed3);
				break;
			}
		}
	}

	// 如果宠物什么的如果也有buf，看情况处理：
	// 统一存到一个表格中（使用BFighetId），或者分开存储。
	// 【建议分开处理】。
	public final Bufs GetBufs(long roleId) {
		return new Bufs(roleId, _tbufs.getOrAdd(roleId));
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
    public ModuleBuf(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
