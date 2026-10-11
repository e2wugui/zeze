package GameSim;

import java.util.ArrayDeque;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * 玩家模拟：一虚拟线程一玩家，会话亲和——本角色记录的全部事务在本线程执行
 * （交易例外：受要约方线程一笔事务写双方）。
 *
 * <p>确定性纪律（共识 Q6）：本玩家 rng 只在决策点（事务外）推进；事务体携带已定参数，
 * 乐观锁重试重跑事务体不重耗随机数。背包/装备镜像（bagMirror/equipAtk）由本线程在
 * 事务内重同步；战斗 hpMirror 由房间线程写、本线程读，药水治疗本线程写——最后写者胜，
 * Hp 不进守恒口径。
 */
final class PlayerSim implements Runnable {
	final GameWorld world;
	final GameConfig cfg;
	final long roleId;
	private final OpRecorder rec;
	private final Random rng; // 种子=roleId

	// 收件箱（跨线程投递：loot/死亡/交易要约/邮件通知）
	final ConcurrentLinkedQueue<Object> inbox = new ConcurrentLinkedQueue<>();
	// 自有邮箱（mailKey 队列，仅本线程读写）
	private final ArrayDeque<Long> mailbox = new ArrayDeque<>();

	// 内存镜像
	final int[] bagMirror = new int[GameConfig.BagSlots]; // 0=空
	volatile int equipAtk;
	volatile int hpMirror = GameConfig.PlayerMaxHp;
	volatile long lastActiveGameMs; // 心跳扫描读
	volatile boolean loggedIn;
	volatile boolean kicked;      // 心跳扫描踢线 → 下轮 step 重登
	volatile boolean inRoom;
	private volatile CompletableFuture<Void> roomFuture;

	private long[] friendIds = new long[0]; // 登录创建时事务外定死

	// ===== 事件类型（房间/其他玩家投递，参数已定死）=====
	static final class LootEvent {
		final long exp, damage, money;
		final int itemId, count;
		LootEvent(long exp, long damage, int itemId, int count, long money) {
			this.exp = exp;
			this.damage = damage;
			this.itemId = itemId;
			this.count = count;
			this.money = money;
		}
	}

	static final class DeathEvent {
		final long moneyLoss;
		DeathEvent(long moneyLoss) {
			this.moneyLoss = moneyLoss;
		}
	}

	static final class TradeOffer {
		final long from;
		final int slot, itemId, count, money; // 要约方：背包槽位+物品+追加金币
		TradeOffer(long from, int slot, int itemId, int count, int money) {
			this.from = from;
			this.slot = slot;
			this.itemId = itemId;
			this.count = count;
			this.money = money;
		}
	}

	static final class MailNotify {
		final long mailKey;
		MailNotify(long mailKey) {
			this.mailKey = mailKey;
		}
	}

	/** 房间掷骰：定死一次击杀的 loot 参数（45%药水/10%装备/15%金币/30%空） */
	static LootEvent rollLoot(Random rng, long exp, long damage) {
		int roll = rng.nextInt(100);
		if (roll < 45)
			return new LootEvent(exp, damage, GameConfig.ItemPotion, 1 + rng.nextInt(3), 0);
		if (roll < 55)
			return new LootEvent(exp, damage, rng.nextBoolean() ? GameConfig.ItemWeapon : GameConfig.ItemArmor, 1, 0);
		if (roll < 70)
			return new LootEvent(exp, damage, 0, 0, 20 + rng.nextInt(30));
		return new LootEvent(exp, damage, 0, 0, 0);
	}

	PlayerSim(GameWorld world, int index) {
		this.world = world;
		this.cfg = world.cfg;
		this.roleId = index;
		this.rec = OpRecorder.create(index);
		this.rng = new Random(0x5EED_0000L + index); // per-player 固定种子
	}

	@Override
	public void run() {
		try {
			// 开服登录潮摊开（决策点：事务外）
			Thread.sleep(cfg.wall((long)(rng.nextDouble() * GameConfig.LoginRampMs)));
			login();
		while (!world.stopped) {
			try {
				step();
			} catch (Throwable e) {
				// 玩家循环异常兜底计入 errors 总账（借用心跳桶；首例打印堆栈定位）
				if (world.markLoopError())
					e.printStackTrace();
				rec.error(OpRecorder.OpType.heartbeat);
			}
		}
		} catch (InterruptedException ignored) {
		} finally {
			if (loggedIn)
				logout();
		}
	}

	private void step() throws Exception {
		drainInbox();
		if (kicked) {
			kicked = false;
			login(); // 会话行已被扫描器移除，重登（角色已存在，无注入）
		}
		long now = world.gameNow();
		if (now - lastActiveGameMs >= GameConfig.HeartbeatPeriodMs)
			heartbeat(now);
		if (inRoom) {
			// 等房间结束（带超时轮询停机信号）；轮询超时是设计内节奏，不计错误
			try {
				roomFuture.get(cfg.wall(500), TimeUnit.MILLISECONDS);
			} catch (java.util.concurrent.TimeoutException ignored) {
			} catch (java.util.concurrent.ExecutionException e) {
				// 房间 future 只会正常完成（防御）
				if (world.markLoopError())
					e.printStackTrace();
			}
			return;
		}
		idleActivity();
		think();
	}

	private void think() throws InterruptedException {
		Thread.sleep(cfg.wall(GameConfig.ThinkMinMs
				+ (long)(rng.nextDouble() * (GameConfig.ThinkMaxMs - GameConfig.ThinkMinMs))));
	}

	// ===== 生命周期 =====

	private void login() {
		if (loggedIn)
			return;
		// 决策点：好友名单（仅首次创建角色用）
		if (friendIds.length == 0)
			decideFriends();
		var isAdd = new Zeze.Util.OutObject<Boolean>();
		if (!world.call(rec, OpRecorder.OpType.login, "GameSim.Login", () -> {
			var role = world.tRole.getOrAdd(roleId, isAdd);
			if (Boolean.TRUE.equals(isAdd.value)) {
				role.setLevel(1);
				role.setMoney(1000);
				role.setHp(GameConfig.PlayerMaxHp);
				insertItem(role, GameConfig.ItemPotion, 5);
				insertItem(role, GameConfig.ItemWeapon, 1);
				insertItem(role, GameConfig.ItemArmor, 1);
				for (long fid : friendIds)
					role.getFriends().put(fid, 0);
			}
			world.tSession.getOrAdd(roleId).setLastActive(world.gameNow());
			resyncMirror(role);
			return 0L;
		})) {
			return;
		}
		if (Boolean.TRUE.equals(isAdd.value)) { // 新角色系统注入：建号补贴（守恒记账）
			world.ledger.money(1000);
			world.ledger.roleMoney(roleId, 1000);
			world.ledger.item(GameConfig.ItemPotion, 5);
			world.ledger.item(GameConfig.ItemWeapon, 1);
			world.ledger.item(GameConfig.ItemArmor, 1);
		}
		loggedIn = true;
		world.online.put(roleId, this);
	}

	private void decideFriends() {
		friendIds = new long[20];
		int n = 0;
		while (n < friendIds.length) {
			long id = rng.nextInt(cfg.players);
			if (id == roleId)
				continue;
			friendIds[n++] = id;
		}
	}

	private void logout() {
		if (!world.call(rec, OpRecorder.OpType.logout, "GameSim.Logout", () -> {
			world.tSession.remove(roleId);
			return 0L;
		})) {
			return;
		}
		loggedIn = false;
		world.online.remove(roleId, this);
	}

	private void heartbeat(long now) {
		if (world.call(rec, OpRecorder.OpType.heartbeat, "GameSim.Heartbeat", () -> {
			world.tSession.getOrAdd(roleId).setLastActive(now);
			return 0L;
		})) {
			lastActiveGameMs = now;
		}
	}

	// ===== 收件箱：结果事务（单写者亲和——本人线程写本人记录）=====

	private void drainInbox() {
		Object ev;
		while ((ev = inbox.poll()) != null) {
			if (ev instanceof LootEvent e)
				applyLoot(e);
			else if (ev instanceof DeathEvent e)
				applyDeath(e);
			else if (ev instanceof TradeOffer o)
				applyTrade(o);
			else if (ev instanceof MailNotify n)
				mailbox.add(n.mailKey);
		}
	}

	private void applyLoot(LootEvent e) {
		var item = new int[]{e.itemId, e.count}; // 事务内可能因背包满降级为不入
		if (!world.call(rec, OpRecorder.OpType.loot, "GameSim.Loot", () -> {
			item[0] = e.itemId; // 每轮重置（重试轮放弃时防上轮残留幻影记账）
			var role = world.tRole.getOrAdd(roleId);
			role.setExp(role.getExp() + e.exp);
			int oldLevel = role.getLevel();
			while (role.getExp() >= GameConfig.expToNext(role.getLevel())) { // 升级曲线
				role.setExp(role.getExp() - GameConfig.expToNext(role.getLevel()));
				role.setLevel(role.getLevel() + 1);
			}
			if (role.getLevel() != oldLevel)
				submitRank(world.tRank, 2L, oldLevel * RANK_KEY_SCALE + roleId, role.getLevel() * RANK_KEY_SCALE + roleId); // 等级榜
			long before = role.getDamageTotal();
			role.setDamageTotal(before + e.damage);
			submitRank(world.tRank, 1L, before * RANK_KEY_SCALE + roleId, (before + e.damage) * RANK_KEY_SCALE + roleId); // 伤害榜
			if (item[0] != 0 && insertItem(role, item[0], item[1]) < 0)
				item[0] = 0; // 背包满：物品不入（未注入即未销毁，守恒两侧都不记）
			if (e.money > 0)
				role.setMoney(role.getMoney() + e.money);
			resyncMirror(role);
			return 0L;
		})) {
			return;
		}
		if (e.money > 0) {
			world.ledger.money(e.money);
			world.ledger.roleMoney(roleId, e.money);
		}
		if (item[0] != 0)
			world.ledger.item(item[0], item[1]);
	}

	private void applyDeath(DeathEvent e) {
		var paid = new long[]{0};
		if (!world.call(rec, OpRecorder.OpType.loot, "GameSim.DeathPenalty", () -> {
			paid[0] = 0; // 每轮重置
			var role = world.tRole.getOrAdd(roleId);
			long loss = Math.min(role.getMoney(), e.moneyLoss);
			role.setMoney(role.getMoney() - loss);
			paid[0] = loss;
			return 0L;
		})) {
			return;
		}
		world.ledger.money(-paid[0]); // 系统销毁
		world.ledger.roleMoney(roleId, -paid[0]);
	}

	/** 交易：受要约方一笔事务写双方（要约参数已过期即静默放弃——过期是常态非错误） */
	private void applyTrade(TradeOffer o) {
		if (rng.nextInt(100) >= 80)
			return; // 拒绝（决策点）
		int payBack = 5 + rng.nextInt(20); // 还价金币（决策点）
		var moved = new int[]{0};
		if (!world.call(rec, OpRecorder.OpType.trade, "GameSim.Trade", () -> {
			moved[0] = 0; // 每轮重置（重试轮要约过期走放弃时，防上轮 moved 残留）
			var roleA = world.tRole.getOrAdd(o.from);
			var roleB = world.tRole.getOrAdd(roleId);
			if (roleB.getMoney() < payBack)
				return 0L;
			var bagA = roleA.getBag();
			var itemA = bagA.get(o.slot);
			if (itemA == null || itemA.getItemId() != o.itemId || itemA.getCount() < o.count)
				return 0L; // 要约物品已变（过期），放弃
			// 收货容量探测（不改动），随后 insertItem 带堆叠合并入包——
			// 直接 placeItem 到堆叠槽会整槽覆盖（PMap 同键替换=旧堆叠蒸发，守恒验证已实锤）
			if (findSlotFor(roleB, o.itemId, o.count) < 0)
				return 0L; // 背包满，放弃
			// A 方：扣物品+扣金币；B 方反向
			removeAt(bagA, o.slot, o.count);
			roleA.setMoney(roleA.getMoney() - o.money + payBack);
			insertItem(roleB, o.itemId, o.count);
			roleB.setMoney(roleB.getMoney() - payBack + o.money);
			moved[0] = o.count;
			resyncMirror(roleB);
			return 0L;
		})) {
			return;
		}
		// 角色间转移：全局守恒不记，按角色账记（诊断口径）——必须 moved 门控：
		// 静默放弃（要约过期/背包满）返回 0 也是"成功"，无 DB 变动不可记账
		// （角色级对账网实锤：放弃路径记账造成双向小额漂移）
		if (moved[0] != 0) {
			world.ledger.roleMoney(o.from, -o.money + payBack);
			world.ledger.roleMoney(roleId, o.money - payBack);
		}
	}

	// ===== 大厅活动（权重分派，决策全在事务外）=====

	private void idleActivity() throws Exception {
		int r = rng.nextInt(GameConfig.WIdleSum);
		if ((r -= GameConfig.WFriendQuery) < 0)
			friendQuery();
		else if ((r -= GameConfig.WUsePotion) < 0)
			usePotion();
		else if ((r -= GameConfig.WEquipChange) < 0)
			equipChange();
		else if ((r -= GameConfig.WSellShop) < 0)
			sellShop();
		else if ((r -= GameConfig.WMailClaim) < 0)
			mailClaim();
		else if ((r -= GameConfig.WMailSend) < 0)
			mailSend();
		else if ((r -= GameConfig.WTrade) < 0)
			tradePropose();
		else
			adventure();
	}

	/** 好友在线状态查询：纯读事务（读提交簿记的真实载体） */
	private void friendQuery() {
		world.call(rec, OpRecorder.OpType.friendQuery, "GameSim.FriendQuery", () -> {
			var role = world.tRole.get(roleId);
			if (role == null)
				return 0L;
			int n = 0;
			for (var id : role.getFriendsReadOnly().keySet())
				if (world.tSession.get(id) != null)
					++n;
			return 0L;
		});
	}

	private void usePotion() {
		int slot = findMirrorSlot(GameConfig.ItemPotion);
		if (slot < 0 || hpMirror >= GameConfig.PlayerMaxHp * 7 / 10)
			return; // 没药或血量健康：不产生事务（避免空事务放大固定提交成本）
		var used = new boolean[1];
		if (!world.call(rec, OpRecorder.OpType.usePotion, "GameSim.UsePotion", () -> {
			used[0] = false; // 每轮重置
			var role = world.tRole.getOrAdd(roleId);
			var bag = role.getBag();
			var item = bag.get(slot);
			if (item == null || item.getItemId() != GameConfig.ItemPotion) {
				return 0L; // 镜像过期（交易改了背包）：本轮跳过
			}
			removeAt(bag, slot, 1);
			role.setHp(Math.min(GameConfig.PlayerMaxHp, role.getHp() + 30));
			resyncMirror(role);
			used[0] = true;
			return 0L;
		})) {
			return;
		}
		if (used[0]) {
			hpMirror = Math.min(GameConfig.PlayerMaxHp, hpMirror + 30);
			world.ledger.item(GameConfig.ItemPotion, -1); // 系统销毁
		}
	}

	private void equipChange() {
		int slot = findMirrorSlot(GameConfig.ItemWeapon);
		if (slot < 0)
			return;
		var eq = new boolean[1];
		if (!world.call(rec, OpRecorder.OpType.equipChange, "GameSim.Equip", () -> {
			eq[0] = false; // 每轮重置
			var role = world.tRole.getOrAdd(roleId);
			var bag = role.getBag();
			var item = bag.get(slot);
			if (item == null || item.getItemId() != GameConfig.ItemWeapon)
				return 0L;
			var equip = role.getEquip();
			// bean 所有权语义（HasManagedException）：受管 bean 不得跨容器搬移——
			// 一律"取值重建"：旧装备先摘下、数值入新实例回包，新武器同法上架
			var old = equip.get(0);
			int oldCount = 0, oldAtk = 0, oldDef = 0, oldAddHp = 0, oldPrice = 0;
			if (old != null) {
				oldCount = old.getCount();
				oldAtk = old.getAtk();
				oldDef = old.getDef();
				oldAddHp = old.getAddHp();
				oldPrice = old.getPrice();
			}
			bag.remove(slot);
			if (old != null) {
				equip.remove(0);
				placeItem(role, slot, GameConfig.ItemWeapon, oldCount, oldAtk, oldDef, oldAddHp, oldPrice);
			}
			placeEquip(role, item);
			resyncMirror(role);
			eq[0] = true;
			return 0L;
		})) {
			return;
		}
		if (eq[0])
			equipAtk = 10; // 武器攻击加成（换装即等值，简化）
	}

	/** 数值重建上架装备栏（不搬 bean） */
	private static void placeEquip(demo.Game.BRole role, demo.Game.BItem from) {
		var item = new demo.Game.BItem();
		item.setItemId(from.getItemId());
		item.setCount(from.getCount());
		item.setAtk(from.getAtk());
		item.setDef(from.getDef());
		item.setAddHp(from.getAddHp());
		item.setPrice(from.getPrice());
		role.getEquip().put(0, item);
	}

	private void sellShop() {
		int slot = pickMirrorOccupied();
		if (slot < 0)
			return;
		var gain = new long[]{0, 0, 0}; // itemId, count, money
		if (!world.call(rec, OpRecorder.OpType.sellShop, "GameSim.SellShop", () -> {
			gain[0] = gain[1] = gain[2] = 0; // 每轮重置（重试轮镜像过期走放弃时，防上轮售价残留）
			var role = world.tRole.getOrAdd(roleId);
			var item = role.getBag().get(slot);
			if (item == null)
				return 0L; // 镜像过期
			gain[0] = item.getItemId();
			gain[1] = item.getCount();
			gain[2] = (long)item.getPrice() * item.getCount();
			role.setMoney(role.getMoney() + gain[2]);
			role.getBag().remove(slot);
			resyncMirror(role);
			return 0L;
		})) {
			return;
		}
		if (gain[1] > 0) { // 卖店=系统注入金币、销毁物品
			world.ledger.money(gain[2]);
			world.ledger.roleMoney(roleId, gain[2]);
			world.ledger.item((int)gain[0], -gain[1]);
		}
	}

	private static int itemPrice(int itemId) {
		return switch (itemId) {
			case GameConfig.ItemPotion -> 5;
			case GameConfig.ItemWeapon -> 100;
			case GameConfig.ItemArmor -> 80;
			default -> 0;
		};
	}

	private void mailClaim() {
		if (mailbox.isEmpty())
			return;
		long key = mailbox.poll();
		var state = new int[]{ST_MISSING}; // 事务结局：missing/claimed/full
		var mailMoney = new long[1];
		if (!world.call(rec, OpRecorder.OpType.mailClaim, "GameSim.MailClaim", () -> {
			state[0] = ST_MISSING; // 每轮重置
			mailMoney[0] = 0;
			var m = world.tMail.get(key);
			if (m == null) {
				return 0L; // 已被领（重复通知防御）：丢弃
			}
			var role = world.tRole.getOrAdd(roleId);
			for (var entry : m.getItems().entrySet()) { // 先验证附件全可入包
				var it = entry.getValue();
				if (findSlotFor(role, it.getItemId(), it.getCount()) < 0) {
					state[0] = ST_FULL;
					return 0L; // 背包满：整封跳过（重塞队尾）
				}
			}
			for (var entry : m.getItems().entrySet()) {
				var it = entry.getValue();
				insertItem(role, it.getItemId(), it.getCount()); // 堆叠合并（容量已在探测步验证）
			}
			role.setMoney(role.getMoney() + m.getMoney());
			mailMoney[0] = m.getMoney();
			world.tMail.remove(key);
			resyncMirror(role);
			state[0] = ST_CLAIMED;
			return 0L;
		})) {
			mailbox.add(key); // 事务失败（非放弃）：邮件保留重试
			return;
		}
		if (state[0] == ST_CLAIMED) {
			world.addMailClaimed();
			world.ledger.roleMoney(roleId, mailMoney[0]);
		}
		else if (state[0] == ST_FULL)
			mailbox.add(key); // 背包满：稍后再试
		// ST_MISSING：丢弃
	}

	private static final int ST_MISSING = 0, ST_CLAIMED = 1, ST_FULL = 2;

	private void mailSend() {
		if (friendIds.length == 0 || mailboxFull())
			return;
		int slot = findMirrorSlot(GameConfig.ItemPotion);
		if (slot < 0)
			return;
		long friend = friendIds[rng.nextInt(friendIds.length)];
		long key = world.allocMailKey(); // 号段事务外预分配
		var sent = new boolean[1];
		if (!world.call(rec, OpRecorder.OpType.mailSend, "GameSim.MailSend", () -> {
			sent[0] = false; // 每轮重置
			var role = world.tRole.getOrAdd(roleId);
			if (role.getMoney() < 10)
				return 0L;
			var item = role.getBag().get(slot);
			if (item == null || item.getItemId() != GameConfig.ItemPotion)
				return 0L; // 镜像过期
			removeAt(role.getBag(), slot, 1);
			role.setMoney(role.getMoney() - 10);
			var m = world.tMail.getOrAdd(key);
			m.setFrom(roleId);
			m.setMoney(10);
			m.getItems().clear();
			var attach = new demo.Game.BItem();
			attach.setItemId(GameConfig.ItemPotion);
			attach.setCount(1);
			attach.setAddHp(30);
			attach.setPrice(5);
			m.getItems().put(0, attach);
			resyncMirror(role);
			sent[0] = true;
			return 0L;
		})) {
			return;
		}
		if (sent[0]) {
			world.ledger.roleMoney(roleId, -10);
			world.addMailInserted();
			world.onlineNotify(friend, key); // 提交后通知（角色↔邮件转移，守恒不记）
		}
	}

	private void tradePropose() {
		if (friendIds.length == 0)
			return;
		int slot = pickMirrorOccupied();
		if (slot < 0)
			return;
		int itemId = bagMirror[slot];
		int money = rng.nextInt(15);
		long partner = friendIds[rng.nextInt(friendIds.length)];
		var p = world.online.get(partner);
		if (p != null && p != this)
			p.inbox.add(new TradeOffer(roleId, slot, itemId, 1, money));
		// 对方拒绝/过期是常态：无本地状态，无对账需求
	}

	private void adventure() {
		roomFuture = new CompletableFuture<>();
		inRoom = true;
		world.joinMatchmaking(this);
	}

	void roomDone() {
		inRoom = false;
		var f = roomFuture;
		if (f != null)
			f.complete(null);
	}

	// ===== 背包原语（事务内调用）=====

	/** 插入物品：可堆叠先找同 id 槽，否则找空槽；失败返回 -1 */
	private static int insertItem(demo.Game.BRole role, int itemId, int count) {
		var bag = role.getBag();
		if (itemId == GameConfig.ItemPotion) { // 药水可堆叠
			for (var e : bag.entrySet()) {
				var it = e.getValue();
				if (it.getItemId() == itemId) {
					it.setCount(it.getCount() + count);
					return e.getKey();
				}
			}
		}
		for (int s = 0; s < GameConfig.BagSlots; s++) {
			if (!bag.containsKey(s)) {
				placeItem(role, s, itemId, count, itemAtk(itemId), itemDef(itemId), itemAddHp(itemId), itemPrice(itemId));
				return s;
			}
		}
		return -1;
	}

	private static int findSlotFor(demo.Game.BRole role, int itemId, int count) {
		var bag = role.getBag();
		if (itemId == GameConfig.ItemPotion) {
			for (var e : bag.entrySet())
				if (e.getValue().getItemId() == itemId)
					return e.getKey();
		}
		for (int s = 0; s < GameConfig.BagSlots; s++)
			if (!bag.containsKey(s))
				return s;
		return -1;
	}

	private static void placeItem(demo.Game.BRole role, int slot, int itemId, int count,
								  int atk, int def, int addHp, int price) {
		var item = new demo.Game.BItem();
		item.setItemId(itemId);
		item.setCount(count);
		item.setAtk(atk);
		item.setDef(def);
		item.setAddHp(addHp);
		item.setPrice(price);
		role.getBag().put(slot, item);
	}

	/** 从槽位扣 count 个（数量归零移除槽位） */
	private static void removeAt(Zeze.Transaction.Collections.PMap2<Integer, demo.Game.BItem> bag, int slot, int count) {
		var it = bag.get(slot);
		if (it.getCount() > count)
			it.setCount(it.getCount() - count);
		else
			bag.remove(slot);
	}

	private static int itemAtk(int itemId) {
		return itemId == GameConfig.ItemWeapon ? 10 : 0;
	}

	private static int itemDef(int itemId) {
		return itemId == GameConfig.ItemArmor ? 8 : 0;
	}

	private static int itemAddHp(int itemId) {
		return itemId == GameConfig.ItemPotion ? 30 : 0;
	}

	/** 排行键=score×2^20+roleId（roleId<2^20 保证唯一、按键有序即按分排序） */
	static final long RANK_KEY_SCALE = 1_048_576L;

	/** 排行提交：删旧键放新键；容量 100 超出删最低分 */
	private static void submitRank(demo.Game.tRank tRank, long boardId, long oldKey, long newKey) {
		var entries = tRank.getOrAdd(boardId).getEntries();
		entries.remove(oldKey);
		entries.put(newKey, newKey % RANK_KEY_SCALE); // value=roleId（低 20 位）
		if (entries.size() > 100)
			entries.remove(entries.firstKey());
	}

	/** 事务内重同步背包/装备镜像（本线程读写） */
	private void resyncMirror(demo.Game.BRole role) {
		var bag = role.getBag();
		for (int s = 0; s < GameConfig.BagSlots; s++) {
			var it = bag.get(s);
			bagMirror[s] = it == null ? 0 : it.getItemId();
		}
		equipAtk = role.getEquip().get(0) != null ? 10 : 0;
	}

	// ===== 镜像查询（决策点，事务外）=====

	private int findMirrorSlot(int itemId) {
		for (int s = 0; s < bagMirror.length; s++)
			if (bagMirror[s] == itemId)
				return s;
		return -1;
	}

	private int pickMirrorOccupied() {
		int n = 0;
		for (int itemId : bagMirror)
			if (itemId != 0)
				++n;
		if (n == 0)
			return -1;
		int k = rng.nextInt(n);
		for (int s = 0; s < bagMirror.length; s++) {
			if (bagMirror[s] != 0 && k-- == 0)
				return s;
		}
		return -1;
	}

	private boolean mailboxFull() {
		return mailbox.size() >= 32;
	}
}
