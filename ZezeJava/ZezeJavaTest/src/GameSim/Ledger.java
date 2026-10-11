package GameSim;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * 守恒不变式的影子账本：只记系统注入/销毁（角色↔邮件之间的转移两侧相抵无需记账）。
 * 每笔成功事务提交后（事务外）记账；末轮外与数据库全量重算对账。
 *
 * <p>守恒公式（验证在 LMmoGameSim.verifyConservation）：
 * Σrole.Money + Σ未领邮件.Money == moneyNet；
 * Σ(背包+装备栏+未领邮件附件).count[itemId] == itemNet[itemId]。
 */
public final class Ledger {
	private final LongAdder moneyNet = new LongAdder();
	private final ConcurrentHashMap<Integer, LongAdder> itemNet = new ConcurrentHashMap<>();
	// 按角色货币账（诊断用：含转移流，role.Money 应恰等于建号补贴+净变动）
	private final ConcurrentHashMap<Long, LongAdder> roleMoney = new ConcurrentHashMap<>();

	public void money(long delta) {
		moneyNet.add(delta);
	}

	/** 按角色记账（转移流也记：交易/领信/发信；验证时与 DB 逐角色对） */
	public void roleMoney(long roleId, long delta) {
		roleMoney.computeIfAbsent(roleId, k -> new LongAdder()).add(delta);
	}

	public long roleMoneyOf(long roleId) {
		var a = roleMoney.get(roleId);
		return a == null ? 0 : a.sum();
	}

	public void item(int itemId, long delta) {
		itemNet.computeIfAbsent(itemId, k -> new LongAdder()).add(delta);
	}

	public long moneyNet() {
		return moneyNet.sum();
	}

	public long itemNet(int itemId) {
		var a = itemNet.get(itemId);
		return a == null ? 0 : a.sum();
	}

	/** 对账用快照（itemId → 净注入） */
	public ConcurrentHashMap<Integer, LongAdder> items() {
		return itemNet;
	}
}
