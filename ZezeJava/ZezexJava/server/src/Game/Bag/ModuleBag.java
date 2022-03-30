package Game.Bag;

import Zeze.Net.Protocol;
import Zeze.Transaction.*;
import Game.*;
import Zeze.Util.Reflect;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class ModuleBag extends AbstractModule {
	public void Start(App app) {
		_tbag.getChangeListenerMap().AddListener(tbag.VAR_Items, new ItemsChangeListener());
		_tbag.getChangeListenerMap().AddListener(tbag.VAR_All, new BagChangeListener());
	}

	public void Stop(App app) {
	}

	private static class BagChangeListener implements ChangeListener {
		private static String Name = "Game.Bag";
		public static String getName() {
			return Name;
		}

		public final void OnChanged(Object key, Bean value) {
			// 记录改变，通知全部。
			BBag bbag = (BBag)value;
			var sbag = new SBag();
			Bag.ToProtocol(bbag, sbag.Argument);

			Game.App.getInstance().Game_Login.getOnlines().SendReliableNotify((Long)key, getName(), sbag);
		}

		public final void OnChanged(Object key, Bean value, ChangeNote note) {
			// 整个记录改变没有 note，只有Map,Set才有note。
			OnChanged(key, value);
		}

		public final void OnRemoved(Object key) {
			SChanged changed = new SChanged();
			changed.Argument.setChangeTag(BChangedResult.ChangeTagRecordIsRemoved);
			Game.App.getInstance().Game_Login.getOnlines().SendReliableNotify((Long)key, getName(), changed);
		}
	}

	private static class ItemsChangeListener implements ChangeListener {
		public final String getName() {
			return BagChangeListener.getName();
		}

		public final void OnChanged(Object key, Bean value) {
			// 整个记录改变，由 BagChangeListener 处理。发送包含 Money, Capacity.
		}

		public final void OnChanged(Object key, Bean value, ChangeNote note) {
			// 增量变化，通知变更。
			@SuppressWarnings("unchecked")
			ChangeNoteMap2<Integer, BItem> notemap2 = (ChangeNoteMap2<Integer, BItem>)note;
			BBag bbag = (BBag)value;
			notemap2.MergeChangedToReplaced(bbag.getItems());

			SChanged changed = new SChanged();
			changed.Argument.setChangeTag(BChangedResult.ChangeTagNormalChanged);
			changed.Argument.getItemsReplace().putAll(notemap2.getReplaced());
			for (var p : notemap2.getRemoved()) {
				changed.Argument.getItemsRemove().add(p);
			}

			Game.App.getInstance().Game_Login.getOnlines().SendReliableNotify((Long)key, getName(), changed);
		}

		public final void OnRemoved(Object key) {
			// 整个记录删除，由 BagChangeListener 处理。
		}
	}

	// protocol handles
	@Override
	protected long ProcessMoveRequest(Move rpc) throws Throwable {
		var session = Game.Login.Session.Get(rpc);
		// throw exception if not login
		var moduleCode = GetBag(session.getRoleId().longValue()).Move(
				rpc.Argument.getPositionFrom(), rpc.Argument.getPositionTo(), rpc.Argument.getNumber());
		if (moduleCode != 0) {
			return ErrorCode(moduleCode);
		}
		session.SendResponse(rpc);
		return 0;
	}

	@Override
	protected long ProcessDestroyRequest(Destroy rpc) throws Throwable {
		var session = Game.Login.Session.Get(rpc);
		var moduleCode = GetBag(session.getRoleId().longValue()).Destory(rpc.Argument.getPosition());
		if (0 != moduleCode) {
			return ErrorCode(moduleCode);
		}
		session.SendResponse(rpc);
		return 0;
	}

	@Override
	protected long ProcessSortRequest(Sort rpc) throws Throwable {
		var session = Game.Login.Session.Get(rpc);
		Bag bag = GetBag(session.getRoleId().longValue());
		bag.Sort(null);
		session.SendResponse(rpc);
		return Procedure.Success;
	}

	@Override
	protected long ProcessGetBagRequest(GetBag rpc) throws Throwable {
		var session = Game.Login.Session.Get(rpc);

		GetBag(session.getRoleId().longValue()).ToProtocol(rpc.Result);
		session.SendResponse(rpc);
		App.getInstance().Game_Login.getOnlines().AddReliableNotifyMark(session.getRoleId().longValue(), BagChangeListener.getName());
		return Procedure.Success;
	}

	// for other module
	public Bag GetBag(long roleid) {
		return new Bag(roleid, _tbag.getOrAdd(roleid));
	}

	@Override
	protected long ProcessCUse(CUse protocol) throws Throwable {
		var session = Game.Login.Session.Get(protocol);
		Bag bag = GetBag(session.getRoleId().longValue());
		var item = bag.GetItem(protocol.Argument.getPosition());
		if (null != item && item.Use()) {
			if (bag.Remove(protocol.Argument.getPosition(), item.getId(), 1)) {
				return Procedure.Success;
			}
		}
		return Procedure.LogicError;

	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
    public ModuleBag(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
