package Game.Bag;

import Zeze.Transaction.*;
import Game.*;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class ModuleBag extends AbstractModule {
	public void Start(App app) {
		_tbag.ChangeListenerMap.AddListener(tbag.VAR_Items, new ItemsChangeListener());
		_tbag.ChangeListenerMap.AddListener(tbag.VAR_All, new BagChangeListener());
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
			Bag.ToProtocol(bbag, sbag.getArgument());

			App.getInstance().getGameLogin().getOnlines().SendReliableNotify((Long)key, getName(), sbag);
		}

		public final void OnChanged(Object key, Bean value, ChangeNote note) {
			// 整个记录改变没有 note，只有Map,Set才有note。
			OnChanged(key, value);
		}

		public final void OnRemoved(Object key) {
			SChanged changed = new SChanged();
			changed.getArgument().ChangeTag = BChangedResult.ChangeTagRecordIsRemoved;
			App.getInstance().getGameLogin().getOnlines().SendReliableNotify((Long)key, getName(), changed);
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
			ChangeNoteMap2<Integer, BItem> notemap2 = (ChangeNoteMap2<Integer, BItem>)note;
			BBag bbag = (BBag)value;
			notemap2.MergeChangedToReplaced(bbag.getItems());

			SChanged changed = new SChanged();
			changed.getArgument().ChangeTag = BChangedResult.ChangeTagNormalChanged;

			changed.getArgument().getItemsReplace().AddRange(notemap2.Replaced);
			for (var p : notemap2.Removed) {
				changed.getArgument().getItemsRemove().Add(p);
			}

			App.getInstance().getGameLogin().getOnlines().SendReliableNotify((Long)key, getName(), changed);
		}

		public final void OnRemoved(Object key) {
			// 整个记录删除，由 BagChangeListener 处理。
		}
	}

	// protocol handles
	@Override
	public int ProcessMoveRequest(Move rpc) {
		Login.Session session = Login.Session.Get(rpc);
		// throw exception if not login
		var moduleCode = GetBag(session.getRoleId().longValue()).Move(rpc.getArgument().getPositionFrom(), rpc.getArgument().getPositionTo(), rpc.getArgument().getNumber());
		if (moduleCode != 0) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: return ReturnCode((ushort)moduleCode);
			return ReturnCode((short)moduleCode);
		}
		session.SendResponse(rpc);
		return 0;
	}

	@Override
	public int ProcessDestroyRequest(Destroy rpc) {
		Login.Session session = Login.Session.Get(rpc);
		var moduleCode = GetBag(session.getRoleId().longValue()).Destory(rpc.getArgument().getPosition());
		if (0 != moduleCode) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: return ReturnCode((ushort)moduleCode);
			return ReturnCode((short)moduleCode);
		}
		session.SendResponse(rpc);
		return 0;
	}

	@Override
	public int ProcessSortRequest(Sort rpc) {
		Login.Session session = Login.Session.Get(rpc);
		Bag bag = GetBag(session.getRoleId().longValue());
		bag.Sort();
		session.SendResponse(rpc);
		return Procedure.Success;
	}

	@Override
	public int ProcessGetBagRequest(GetBag rpc) {
		Login.Session session = Login.Session.Get(rpc);

		GetBag(session.getRoleId().longValue()).ToProtocol(rpc.getResult());
		session.SendResponse(rpc);
		App.getInstance().getGameLogin().getOnlines().AddReliableNotifyMark(session.getRoleId().longValue(), BagChangeListener.getName());
		return Procedure.Success;
	}

	// for other module
	public Bag GetBag(long roleid) {
		return new Bag(roleid, _tbag.GetOrAdd(roleid));
	}

	@Override
	public int ProcessCUse(CUse protocol) {
		Login.Session session = Login.Session.Get(protocol);
		Bag bag = GetBag(session.getRoleId().longValue());
		Item.Item item = bag.GetItem(protocol.getArgument().getPosition());
		if (null != item && item.Use()) {
			if (bag.Remove(protocol.getArgument().getPosition(), item.getId(), 1)) {
				return Procedure.Success;
			}
		}
		return Procedure.LogicError;

	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
	public static final int ModuleId = 2;

	private tbag _tbag = new tbag();

	private App App;
	public App getApp() {
		return App;
	}

	public ModuleBag(App app) {
		App = app;
		// register protocol factory and handles
		getApp().getServer().AddFactoryHandle(184003, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Game.Bag.CUse(), Handle = Zeze.Net.Service.<CUse>MakeHandle(this, this.getClass().getMethod("ProcessCUse"))});
		getApp().getServer().AddFactoryHandle(175038, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Game.Bag.Destroy(), Handle = Zeze.Net.Service.<Destroy>MakeHandle(this, this.getClass().getMethod("ProcessDestroyRequest"))});
		getApp().getServer().AddFactoryHandle(137439, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Game.Bag.GetBag(), Handle = Zeze.Net.Service.<GetBag>MakeHandle(this, this.getClass().getMethod("ProcessGetBagRequest"))});
		getApp().getServer().AddFactoryHandle(192909, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Game.Bag.Move(), Handle = Zeze.Net.Service.<Move>MakeHandle(this, this.getClass().getMethod("ProcessMoveRequest"))});
		getApp().getServer().AddFactoryHandle(142072, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Game.Bag.Sort(), Handle = Zeze.Net.Service.<Sort>MakeHandle(this, this.getClass().getMethod("ProcessSortRequest"))});
		// register table
		getApp().getZeze().AddTable(getApp().getZeze().Config.GetTableConf(_tbag.Name).DatabaseName, _tbag);
	}

	@Override
	public void UnRegister() {
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__ = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(184003, tempOut__);
	_ = tempOut__.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__2 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(175038, tempOut__2);
	_ = tempOut__2.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__3 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(137439, tempOut__3);
	_ = tempOut__3.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__4 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(192909, tempOut__4);
	_ = tempOut__4.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__5 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(142072, tempOut__5);
	_ = tempOut__5.outArgValue;
		getApp().getZeze().RemoveTable(getApp().getZeze().Config.GetTableConf(_tbag.Name).DatabaseName, _tbag);
	}
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}