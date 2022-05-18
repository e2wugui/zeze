package Game.Bag;

import Zeze.Arch.ProviderUserSession;
import Zeze.Transaction.*;
import Game.*;
import Zeze.Transaction.Collections.LogMap2;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class ModuleBag extends AbstractModule {
	public void Start(App app) {
		_tbag.getChangeListenerMap().AddListener(new BagChangeListener());
	}

	public void Stop(App app) {
	}

	private static class BagChangeListener implements ChangeListener {
		private static final String Name = "Game.Bag";
		public static String getName() {
			return Name;
		}

		@Override
		public final void OnChanged(Object key, Changes.Record c) {
			switch (c.getState()) {
			case Changes.Record.Put:
				// 记录改变，通知全部。
				BBag bbag = (BBag)c.getValue();
				var sbag = new SBag();
				Bag.ToProtocol(bbag, sbag.Argument);
				Game.App.getInstance().getProvider().Online.sendReliableNotify((Long)key, getName(), sbag);
				break;

			case Changes.Record.Edit:
				// 增量变化，通知变更。
				@SuppressWarnings("unchecked")
				var notemap2 = (LogMap2<Integer, BItem>)c.getVariableLog(tbag.VAR_Items);
				if (null != notemap2) {
					notemap2.MergeChangedToReplaced();
					SChanged changed = new SChanged();
					changed.Argument.setChangeTag(BChangedResult.ChangeTagNormalChanged);
					changed.Argument.getItemsReplace().putAll(notemap2.getReplaced());
					for (var p : notemap2.getRemoved()) {
						changed.Argument.getItemsRemove().add(p);
					}
					Game.App.getInstance().getProvider().Online.sendReliableNotify((Long)key, getName(), changed);
				}
				break;

			case Changes.Record.Remove:
				SChanged changed2 = new SChanged();
				changed2.Argument.setChangeTag(BChangedResult.ChangeTagRecordIsRemoved);
				Game.App.getInstance().getProvider().Online.sendReliableNotify((Long)key, getName(), changed2);
				break;
			}
		}
	}

	// protocol handles
	@Override
	protected long ProcessMoveRequest(Move rpc) throws Throwable {
		var session = ProviderUserSession.Get(rpc);
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
		var session = ProviderUserSession.Get(rpc);
		var moduleCode = GetBag(session.getRoleId().longValue()).Destory(rpc.Argument.getPosition());
		if (0 != moduleCode) {
			return ErrorCode(moduleCode);
		}
		session.SendResponse(rpc);
		return 0;
	}

	@Override
	protected long ProcessSortRequest(Sort rpc) throws Throwable {
		var session = ProviderUserSession.Get(rpc);
		Bag bag = GetBag(session.getRoleId().longValue());
		bag.Sort(null);
		session.SendResponse(rpc);
		return Procedure.Success;
	}

	@Override
	protected long ProcessGetBagRequest(GetBag rpc) throws Throwable {
		var session = ProviderUserSession.Get(rpc);

		GetBag(session.getRoleId().longValue()).ToProtocol(rpc.Result);
		session.SendResponse(rpc);
		Game.App.getInstance().getProvider().Online.addReliableNotifyMark(session.getRoleId().longValue(), BagChangeListener.getName());
		return Procedure.Success;
	}

	// for other module
	public Bag GetBag(long roleid) {
		return new Bag(roleid, _tbag.getOrAdd(roleid));
	}

	@Override
	protected long ProcessCUse(CUse protocol) throws Throwable {
		var session = ProviderUserSession.Get(protocol);
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
