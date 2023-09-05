package Game.Equip;

import Game.Fight.*;
import Zeze.Arch.ProviderUserSession;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Hot.HotService;
import Zeze.Transaction.*;
import Game.*;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Util.OutInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

/**
 * todo 这是初始版本。
 */

public final class ModuleEquip extends AbstractModule implements IModuleEquip {
	private static final Logger logger = LogManager.getLogger(ModuleEquip.class);

	String timerHot;
	String timerNamed = "ZezexJava.ModuleEquip.HotTimer.Test";
	String timerOnline;
	long roleId;

	int onlineTimerCount;
	int namedTimerCount;
	int hotTimerCount;
	@Override
	public int increateAndGetOnlineTimerCount() {
		onlineTimerCount += 1;
		return onlineTimerCount;
	}

	@Override
	public int increateAndGetNamedTimerCount() {
		namedTimerCount += 1;
		return namedTimerCount;
	}

	@Override
	public int increateAndGetHotTimerCount() {
		hotTimerCount += 1;
		return hotTimerCount;
	}


	@Override
	public long getRoleId() {
		return roleId;
	}

	private boolean isHotUpgrade() {
		var hotManager = App.Zeze.getHotManager();
		if (null == hotManager)
			return false;
		return hotManager.isUpgrading();
	}

	@Override
	public void StartLast() {

	}

	public void Start(App app) {
		_tequip.getChangeListenerMap().addListener(new ItemsChangeListener());
		app.Zeze.newProcedure(() -> {
			var timer = app.Zeze.getTimer();
			var rand = Zeze.Util.Random.getInstance();
			timer.scheduleNamed(timerNamed,
					rand.nextLong(3000) + 1000,
					rand.nextLong(3000) + 1000,
					HotTimer.class, new BEquipExtra(0, 2, 0));

			if (!isHotUpgrade()) {
				timerHot = timer.schedule(
						rand.nextLong(3000) + 1000,
						rand.nextLong(3000) + 1000,
						HotTimer.class, new BEquipExtra(0, 1, 0));
			}
			resetCollections();
			return 0;
		}, "register timers").call();
	}

	public void Stop(App app) {
		app.Zeze.newProcedure(() -> {
			var timer = app.Zeze.getTimer();
			timer.cancel(timerNamed);
			if (!isHotUpgrade()) {
				timer.cancel(timerHot);
			}
			timer.getRoleTimer().cancel(timerOnline);
			return 0;
		}, "cancel timers").call();
	}

	@Override
	public String getTimerHot() {
		return timerHot;
	}

	public static class HotTimer implements TimerHandle {

		@Override
		public void onTimer(@NotNull TimerContext context) throws Exception {
			var mc = context.timer.zeze.getHotManager().getModuleContext("Game.Equip", IModuleEquip.class);
			var equip = mc.getService();
			var count = 1; // default is 1
			var custom = (BEquipExtra)context.customData;
			switch (custom.getDefence()) {
			case 1:
				count = equip.increateAndGetHotTimerCount();
				break;
			case 2:
				count = equip.increateAndGetNamedTimerCount();
				break;
			case 3:
				count = equip.increateAndGetOnlineTimerCount();
				break;
			}
			if (count != custom.getAttack() + 1)
				throw new RuntimeException("HotTimer verify fail. type=" + custom.getDefence());
			custom.setAttack(count);
		}

		@Override
		public void onTimerCancel() throws Exception {

		}
	}

	private void accessWillRemoveTable() {
		// 访问一下将被热更删除的表。
		var rremove = _tHotRemove.getOrAdd(1L);
		rremove.setAttack(123);
	}

	private static void accessWillRemoveVar(BEquipExtra record) {
		record.setHotRemoveVar(record.getHotRemoveVar() + 1); // 访问将被删除的变量。
	}

	private void resetCollections() {
		var linkedMap = App.LinkedMapModule.open("ZezexJava.HotTest.LinkedMap", BEquipExtra.class);
		linkedMap.remove(String.valueOf(0));
		var queue = App.Zeze.getQueueModule().open("ZezexJava.HotTest.Queue", BEquipExtra.class);
		queue.clear();
		var departmentTree = App.DepartmentTreeModule.open("ZezexJava.HotTest.DT",
				BEquipExtra.class, BEquipExtra.class, BEquipExtra.class, BEquipExtra.class, BEquipExtra.class);
		departmentTree.destroy();
	}

	private void verifyCollections(int oldAccess) {
		{
			var linkedMap = App.LinkedMapModule.open("ZezexJava.HotTest.LinkedMap", BEquipExtra.class);
			var version0 = linkedMap.getOrAdd(String.valueOf(0));
			if (version0.getAttack() != oldAccess)
				throw new RuntimeException("LinkedMap error oldAccess=" + version0.getAttack() + ":" + oldAccess);
			version0.setAttack(oldAccess + 1);
		}
		{
			var queue = App.Zeze.getQueueModule().open("ZezexJava.HotTest.Queue", BEquipExtra.class);
			var old = queue.poll();
			if (old != null && oldAccess != old.getAttack())
				throw new RuntimeException("Queue error oldAccess=" + old.getAttack() + ":" + oldAccess);
			var cur = new BEquipExtra();
			cur.setAttack(oldAccess + 1);
			queue.add(cur);
		}
		{
			var departmentTree = App.DepartmentTreeModule.open("ZezexJava.HotTest.DT",
					BEquipExtra.class, BEquipExtra.class, BEquipExtra.class, BEquipExtra.class, BEquipExtra.class);
			var root = departmentTree.getRoot();
			var next = String.valueOf(oldAccess + 1);
			if (null == root) {
				departmentTree.create().setRoot(next);
			} else {
				if (!root.getRoot().equals(String.valueOf(oldAccess)))
					throw new RuntimeException("Dt error oldAcces=" + root.getRoot() + ":" + oldAccess);
				root.setRoot(next);
			}
		}
	}

	@Override
	public int hotHelloWorld(int oldAccess) {
		var version = new OutInt(oldAccess);
		App.Zeze.newProcedure(() -> {
			accessWillRemoveTable();

			var record = _tHotTest.getOrAdd(1L);
			record.setAttack(record.getAttack() + 1);

			accessWillRemoveVar(record);

			verifyOnlineLocal(oldAccess);
			verifyOnlineUserData(oldAccess);
			verifyCollections(oldAccess);
			verifyMemory(oldAccess);

			version.value = oldAccess + 1;
			return 0;
		}, "").call();
		return version.value;
	}

	private void verifyMemory(int oldAccess) {
		var retreat = _tMemoryRetreat.getOrAdd(1L);
		if (retreat.getAttack() != oldAccess)
			throw new RuntimeException("memoryRetreat error oldAccess=" + retreat.getAttack() + ":" + oldAccess);
		retreat.setAttack(oldAccess + 1);

		// 下面两种情况不做严格测试了，有点麻烦，大概率没什么问题。
		// 简单访问一下，意思意思。
		// 1. 热更后切换为后端数据库存储，数据是以前写的时候存在的。
		var memoryToNormal = _tMemoryToNormal.getOrAdd(1L);
		if (0 != memoryToNormal.getAttack())
			throw new RuntimeException("memoryToNormal error oldAccess=" + memoryToNormal.getAttack() + ":" + oldAccess);
		// 2. 热更后切换为内存表，并且初始是空的。
		var normalToMemory = _tNormalToMemory.getOrAdd(1L);
		if (0 != normalToMemory.getAttack())
			throw new RuntimeException("normalToMemory error oldAccess=" + normalToMemory.getAttack() + ":" + oldAccess);
	}

	public void verifyOnlineUserData(int oldAccess) {
		if (roleId == 0)
			return; // 还没有登录报告成功，忽略验证。

		var online = App.Provider.getOnline();
		var bean = online.getUserData(roleId);
		if (null == bean || bean.typeId() == EmptyBean.TYPEID) {
			var data = new BRetreatTestLocal();
			data.setVarInt1(oldAccess + 1);
			online.setUserData(roleId, data);
			return;
		}
		{
			var data = (BRetreatTestLocal)bean;
			if (oldAccess != data.getVarInt1())
				throw new RuntimeException("verify online data fail.");
			data.setVarInt1(oldAccess + 1);
		}
	}

	public void verifyOnlineLocal(int oldAccess) {
		if (roleId == 0)
			return; // 还没有登录报告成功，忽略验证。

		var key = "ZezexJava.HotTest.Online.Local";
		var online = App.Provider.getOnline();
		var local = (BRetreatTestLocal)online.getLocalBean(roleId, key);
		if (null == local) {
			local = new BRetreatTestLocal();
			local.setVarInt1(oldAccess + 1);
			online.setLocalBean(roleId, key, local);
			return;
		}
		if (oldAccess != local.getVarInt1())
			throw new RuntimeException("verify online local fail.");
		local.setVarInt1(oldAccess + 1);
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
		var oldI = (IModuleEquip)old;
		startOnlineTimer(oldI.getRoleId());
		timerHot = oldI.getTimerHot(); // 继承过来。
		hotTimerCount = oldI.increateAndGetHotTimerCount() - 1; // 继承过来。
	}

	private static class ItemsChangeListener implements ChangeListener {
		private static final String Name = "Game.Equip.Items";
		public static String getName() {
			return Name;
		}

		@Override
		public final void OnChanged(Object key, Changes.Record c) {
			switch (c.getState()) {
				case Changes.Record.Put:
					// 记录改变，通知全部。
					BEquips bequips = (BEquips)c.getValue();

					SEquipement changed = new SEquipement();
					changed.Argument.setChangeTag(BChangedResult.ChangeTagRecordChanged);
					changed.Argument.getItemsReplace().putAll(bequips.getItems());

					Game.App.Instance.getProvider().getOnline().sendReliableNotify((Long)key, getName(), changed);
					break;
				case Changes.Record.Edit:
					// 增量变化，通知变更。
					@SuppressWarnings("unchecked")
					var notemap2 = (LogMap2<Integer, BItem>)c.getVariableLog(tequip.VAR_Items);
					if (null != notemap2) {
						notemap2.mergeChangedToReplaced();
						SEquipement changed2 = new SEquipement();
						changed2.Argument.setChangeTag(BChangedResult.ChangeTagNormalChanged);
						changed2.Argument.getItemsReplace().putAll(notemap2.getReplaced());
						for (var p : notemap2.getRemoved()) {
							changed2.Argument.getItemsRemove().add(p);
						}
						Game.App.Instance.getProvider().getOnline().sendReliableNotify((Long)key, getName(), changed2);
					}
					break;
				case Changes.Record.Remove:
					SEquipement changed3 = new SEquipement();
					changed3.Argument.setChangeTag(BChangedResult.ChangeTagRecordIsRemoved);
					Game.App.Instance.getProvider().getOnline().sendReliableNotify((Long)key, getName(), changed3);
					break;
			}
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
	protected long ProcessEquipementRequest(Equipement rpc) throws Exception {
		var session = ProviderUserSession.get(rpc);
		/*
		Game.Bag.Bag bag = App.Game_Bag.GetBag(session.getRoleId().longValue());
		var bItem = bag.getItems().get(rpc.Argument.getBagPos());
		if (null != bItem) {
			int equipPos = GetEquipPosition(bItem.getId());
			if (equipPos < 0) {
				return errorCode(ResultCodeCannotEquip);
			}

			BEquips equips = _tequip.getOrAdd(session.getRoleId().longValue());
			Game.Bag.BItem bEquipAdd;
			var eItem = equips.getItems().get(equipPos);
			if (null != eItem) {
				// 装备目标位置已经存在装备，交换。
				// 先都删除，这样就能在原位置上交换的装备，否则对于包裹可能加到其他位置。
				equips.getItems().remove(equipPos);
				bag.Remove(rpc.Argument.getBagPos(), bItem.getId(), 1);

				Game.Bag.BItem tempVar = new Game.Bag.BItem();
				tempVar.setId(eItem.getId());
				tempVar.setNumber(1);
				tempVar.setExtra(eItem.getExtra_Game_Equip_BEquipExtra().Copy());
				bag.Add(rpc.Argument.getBagPos(), tempVar);
			}
			else {
				// 装备目标位置为空
				bag.Remove(rpc.Argument.getBagPos(), bItem.getId(), 1);
			}
			bEquipAdd = new BItem();
			bEquipAdd.setId(bItem.getId());
			bEquipAdd.setNumber(1);
			bEquipAdd.setExtra(bItem.getExtra_Game_Equip_BEquipExtra().Copy());
			equips.getItems().put(equipPos, bEquipAdd);
			session.sendResponseWhileCommit(rpc);
			return Procedure.Success;
		}
		*/
		return errorCode(ResultCodeItemNotFound);
	}

	@Override
	protected long ProcessUnequipementRequest(Unequipement rpc) throws Exception {
		var session = ProviderUserSession.get(rpc);
		/*
		BEquips equips = _tequip.getOrAdd(session.getRoleId().longValue());
		var eItem = equips.getItems().get(rpc.Argument.getEquipPos());
		if (null != eItem) {
			equips.getItems().remove(rpc.Argument.getEquipPos());
			var bag = App.Game_Bag.GetBag(session.getRoleId().longValue());
			var bItemAdd = new Game.Bag.BItem();
			bItemAdd.setId(eItem.getId());
			bItemAdd.setNumber(1);
			bItemAdd.setExtra((BEquipExtra)eItem.getExtra().getBean().copy());
			if (0 != bag.Add(-1, bItemAdd)) {
				return errorCode(ResultCodeBagIsFull); // bag is full
			}
			session.sendResponseWhileCommit(rpc);
			return Procedure.Success;
		}
		*/
		return errorCode(ResultCodeEquipNotFound);
	}

	public Game.Item.IItem getEquipItem(long roleId, int position) {
		BEquips equips = _tequip.getOrAdd(roleId);
		return getEquipItem(equips, position);
	}

	public Game.Item.IItem getEquipItem(BEquips equips, int position) {
		var equip = equips.getItems().get(position);
		if (null != equip) {
			var extraTypeId = equip.getExtra().getBean().typeId();
			if (extraTypeId == BEquipExtra.TYPEID)
				return new Equip(equip, (BEquipExtra)equip.getExtra().getBean());
			throw new RuntimeException("unknown extra");
		}
		return null;
	}

	@Override
	public void calculateFighter(IFighter fighter) {
		// todo 加上getId()以后才能用。
		/*
		if (fighter.getId().getType() != BFighterId.TypeRole) {
			return;
		}

		BEquips equips = _tequip.getOrAdd(fighter.getId().getInstanceId());
		for (var pos : equips.getItems().keySet()) {
			var equip = getEquipItem(equips, pos);
			if (null != equip)
				equip.calculateFighter(fighter);
		}
		*/
	}

    @Override
    protected long ProcessSendHotRequest(Game.Equip.SendHot r) {
		var session = ProviderUserSession.get(r);
		session.sendResponseDirect(r);
        return 0;
    }

    @Override
    protected long ProcessSendHotRemoveRequest(Game.Equip.SendHotRemove r) {
		var session = ProviderUserSession.get(r);
		session.sendResponseDirect(r);
		return 0;
    }

    @Override
    protected long ProcessReportLoginRequest(Game.Equip.ReportLogin r) {
		var session = ProviderUserSession.get(r);
		var roleId = session.getRoleId();
		if (roleId == null || roleId != r.Argument.getRoleId() || timerOnline != null)
			return Procedure.LogicError;
		startOnlineTimer(r.Argument.getRoleId());
		session.sendResponseWhileCommit(r);
        return 0;
    }

	private void startOnlineTimer(long roleId) {
		this.roleId = roleId;
		timerOnline = App.Zeze.getTimer().getRoleTimer().scheduleOnlineHot(
				this.roleId, 2000, 2000,
				-1, -1, HotTimer.class, new BEquipExtra(0, 3, 0));
		logger.info("timerOnline=" + timerOnline);
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
    public ModuleEquip(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}
