package Game.Buf;

import Zeze.Transaction.*;
import Game.*;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public class ModuleBuf extends AbstractModule {
	public final void Start(App app) {
		_tbufs.getChangeListenerMap().AddListener(tbufs.VAR_Bufs, new BufChangeListener("Game.Buf.Bufs"));
	}

	public final void Stop(App app) {
	}

	private static class BufChangeListener implements ChangeListener {
		private String Name;
		public final String getName() {
			return Name;
		}

		public BufChangeListener(String name) {
			Name = name;
		}
		public final void OnChanged(Object key, Bean value) {
			// 记录改变，通知全部。
			BBufs record = (BBufs)value;

			SChanged changed = new SChanged();
			changed.Argument.setChangeTag(BBufChanged.ChangeTagRecordChanged);
			changed.Argument.getReplace().putAll(record.getBufs());

			Game.App.Instance.getProvider().Online.sendReliableNotify((Long)key, getName(), changed);
		}

		public final void OnChanged(Object key, Bean value, ChangeNote note) {
			// 增量变化，通知变更。
			@SuppressWarnings("unchecked")
			ChangeNoteMap2<Integer, BBuf> notemap2 = (ChangeNoteMap2<Integer, BBuf>)note;
			BBufs record = (BBufs)value;
			notemap2.MergeChangedToReplaced(record.getBufs());

			SChanged changed = new SChanged();
			changed.Argument.setChangeTag(BBufChanged.ChangeTagNormalChanged);
			changed.Argument.getReplace().putAll(notemap2.getReplaced());
			for (var p : notemap2.getRemoved()) {
				changed.Argument.getRemove().add(p);
			}

			Game.App.getInstance().getProvider().Online.sendReliableNotify((Long)key, getName(), changed);
		}

		public final void OnRemoved(Object key) {
			SChanged changed = new SChanged();
			changed.Argument.setChangeTag(BBufChanged.ChangeTagRecordIsRemoved);
			Game.App.getInstance().getProvider().Online.sendReliableNotify((Long)key, getName(), changed);
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
