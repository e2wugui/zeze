package Game.Equip;

import Game.Fight.*;
import Zeze.Transaction.*;
import Game.*;

// auto-generated


public final class ModuleEquip extends AbstractModule {
	public void Start(App app) {
		_tequip.ChangeListenerMap.AddListener(tequip.VAR_Items, new ItemsChangeListener());
	}

	public void Stop(App app) {
	}

	private static class ItemsChangeListener implements ChangeListener {
		private static String Name = "Game.Equip.Items";
		public static String getName() {
			return Name;
		}

		public final void OnChanged(Object key, Bean value) {
			// 记录改变，通知全部。
			BEquips bequips = (BEquips)value;

			SEquipement changed = new SEquipement();
			changed.getArgument().ChangeTag = Game.Bag.BChangedResult.ChangeTagRecordChanged;
			changed.getArgument().getItemsReplace().AddRange(bequips.getItems());

			App.getInstance().getGameLogin().getOnlines().SendReliableNotify((Long)key, getName(), changed);
		}

		public final void OnChanged(Object key, Bean value, ChangeNote note) {
			// 增量变化，通知变更。
			ChangeNoteMap2<Integer, Game.Bag.BItem> notemap2 = (ChangeNoteMap2<Integer, Game.Bag.BItem>)note;
			BEquips bequips = (BEquips)value;
			notemap2.MergeChangedToReplaced(bequips.getItems());

			SEquipement changed = new SEquipement();
			changed.getArgument().ChangeTag = Game.Bag.BChangedResult.ChangeTagNormalChanged;

			changed.getArgument().getItemsReplace().AddRange(notemap2.Replaced);
			for (var p : notemap2.Removed) {
				changed.getArgument().getItemsRemove().Add(p);
			}

			App.getInstance().getGameLogin().getOnlines().SendReliableNotify((Long)key, getName(), changed);
		}

		public final void OnRemoved(Object key) {
			SEquipement changed = new SEquipement();
			changed.getArgument().ChangeTag = Game.Bag.BChangedResult.ChangeTagRecordIsRemoved;
			App.getInstance().getGameLogin().getOnlines().SendReliableNotify((Long)key, getName(), changed);
		}
	}

	public int GetEquipPosition(int itemId) {
		return 0;
		// 如果装备可以穿到多个位置，则需要选择其中的一个位置返回。
		// 比如戒指，优先返回空的位置，都不为空（可能的规则）返回等级低的位置。
		// 如果物品不能装备到身上的话，返回错误(-1).
		//return -1;
	}
	// 装备只有装上取下两个操作，没有公开的需求，先不提供包装类了。

	@Override
	public int ProcessEquipementRequest(Equipement rpc) {
		Login.Session session = Login.Session.Get(rpc);

		Bag.Bag bag = getApp().Instance.Game_Bag.GetBag(session.getRoleId().longValue());
		Object bItem;
//C# TO JAVA CONVERTER TODO TASK: The following method call contained an unresolved 'out' keyword - these cannot be converted using the 'OutObject' helper class unless the method is within the code being modified:
		if (bag.getItems().TryGetValue(rpc.getArgument().getBagPos(), out bItem)) {
			int equipPos = GetEquipPosition(bItem.Id);
			if (equipPos < 0) {
				return ReturnCode(ResultCodeCannotEquip);
			}

			BEquips equips = _tequip.GetOrAdd(session.getRoleId().longValue());
			Game.Bag.BItem bEquipAdd;
			V eItem;
			tangible.OutObject<V> tempOut_eItem = new tangible.OutObject<V>();
			if (equips.getItems().TryGetValue(equipPos, tempOut_eItem)) {
			eItem = tempOut_eItem.outArgValue;
				// 装备目标位置已经存在装备，交换。
				// 先都删除，这样就能在原位置上交换的装备，否则对于包裹可能加到其他位置。
				equips.getItems().Remove(equipPos);
				bag.Remove(rpc.getArgument().getBagPos(), bItem.Id, 1);

				Bag.BItem tempVar = new Bag.BItem();
				tempVar.setId(eItem.Id);
				tempVar.setNumber(1);
				tempVar.setExtra_Game_Equip_BEquipExtra(eItem.Extra_Game_Equip_BEquipExtra.Copy());
				bag.Add(rpc.getArgument().getBagPos(), tempVar);

				bEquipAdd = new Game.Bag.BItem();
				bEquipAdd.setId(bItem.Id);
				bEquipAdd.setNumber(1);
				bEquipAdd.setExtra_Game_Equip_BEquipExtra(bItem.Extra_Game_Equip_BEquipExtra.Copy());
				equips.getItems().Add(equipPos, bEquipAdd);
			}
			else {
			eItem = tempOut_eItem.outArgValue;
				// 装备目标位置为空
				bag.Remove(rpc.getArgument().getBagPos(), bItem.Id, 1);
				bEquipAdd = new Game.Bag.BItem();
				bEquipAdd.setId(bItem.Id);
				bEquipAdd.setNumber(1);
				bEquipAdd.setExtra_Game_Equip_BEquipExtra(bItem.Extra_Game_Equip_BEquipExtra.Copy());
				equips.getItems().Add(equipPos, bEquipAdd);
			}
			session.SendResponse(rpc);
			return Procedure.Success;
		}
		return ReturnCode(ResultCodeItemNotFound);
	}

	@Override
	public int ProcessUnequipementRequest(Unequipement rpc) {
		Login.Session session = Login.Session.Get(rpc);

		BEquips equips = _tequip.GetOrAdd(session.getRoleId().longValue());
		V eItem;
		tangible.OutObject<V> tempOut_eItem = new tangible.OutObject<V>();
		if (equips.getItems().TryGetValue(rpc.getArgument().getEquipPos(), tempOut_eItem)) {
		eItem = tempOut_eItem.outArgValue;
			equips.getItems().Remove(rpc.getArgument().getEquipPos());
			Bag.Bag bag = getApp().Instance.Game_Bag.GetBag(session.getRoleId().longValue());
			Bag.BItem bItemAdd = new Bag.BItem();
			bItemAdd.setId(eItem.Id);
			bItemAdd.setNumber(1);
			bItemAdd.setExtra_Game_Equip_BEquipExtra((BEquipExtra)eItem.Extra.CopyBean());
			if (0 != bag.Add(-1, bItemAdd)) {
				return ReturnCode(ResultCodeBagIsFull); // bag is full
			}
			session.SendResponse(rpc);
			return Procedure.Success;
		}
	else {
		eItem = tempOut_eItem.outArgValue;
	}

		return ReturnCode(ResultCodeEquipNotFound);
	}

	public Game.Item.Item GetEquipItem(long roleId, int position) {
		BEquips equips = _tequip.GetOrAdd(roleId);
		return GetEquipItem(equips, position);
	}

	public Game.Item.Item GetEquipItem(BEquips equips, int position) {
		V equip;
		tangible.OutObject<V> tempOut_equip = new tangible.OutObject<V>();
		if (equips.getItems().TryGetValue(position, tempOut_equip)) {
		equip = tempOut_equip.outArgValue;
			switch (equip.Extra.TypeId) {
				case BEquipExtra.TYPEID:
					return new Equip(equip, (BEquipExtra)equip.Extra.Bean);
				default:
					throw new RuntimeException("unknown extra");
			}

		}
	else {
		equip = tempOut_equip.outArgValue;
	}
		return null;
	}

	public void CalculateFighter(Fighter fighter) {
		if (fighter.getId().getType() != BFighterId.TypeRole) {
			return;
		}

		BEquips equips = _tequip.GetOrAdd(fighter.getId().getInstanceId());
		for (var pos : equips.getItems().keySet()) {
			GetEquipItem(equips, pos).CalculateFighter(fighter);
		}
	}


	public static final int ModuleId = 7;

	private tequip _tequip = new tequip();

	private App App;
	public App getApp() {
		return App;
	}

	public ModuleEquip(App app) {
		App = app;
		// register protocol factory and handles
		getApp().getServer().AddFactoryHandle(512274, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Game.Equip.Equipement(), Handle = Zeze.Net.Service.<Equipement>MakeHandle(this, this.getClass().getMethod("ProcessEquipementRequest"))});
		getApp().getServer().AddFactoryHandle(483491, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Game.Equip.Unequipement(), Handle = Zeze.Net.Service.<Unequipement>MakeHandle(this, this.getClass().getMethod("ProcessUnequipementRequest"))});
		// register table
		getApp().getZeze().AddTable(getApp().getZeze().Config.GetTableConf(_tequip.Name).DatabaseName, _tequip);
	}

	@Override
	public void UnRegister() {
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__ = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(512274, tempOut__);
	_ = tempOut__.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__2 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(483491, tempOut__2);
	_ = tempOut__2.outArgValue;
		getApp().getZeze().RemoveTable(getApp().getZeze().Config.GetTableConf(_tequip.Name).DatabaseName, _tequip);
	}

}
