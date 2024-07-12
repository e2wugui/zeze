package Onz;

import Zeze.Onz.OnzTransaction;
import demo.Module1.BKuafu;
import demo.Module1.BKuafuResult;

public class KuafuTransaction extends OnzTransaction<BKuafu.Data, BKuafuResult.Data> {
	private final long account1;
	private final long account2;
	private final long money;

	public KuafuTransaction(long account1, long account2, long money) {
		this.account1 = account1;
		this.account2 = account2;
		this.money = money;
	}

	public long m1;
	public long m2;

	@Override
	protected long perform() throws Exception {
		var a1 = new BKuafu.Data();
		a1.setAccount(account1);
		a1.setMoney(money);
		var r1 = new BKuafuResult.Data();
		var future1 = super.callProcedureAsync("zeze1", "kuafu", a1, r1);

		var a2 = new BKuafu.Data();
		a2.setAccount(account1);
		a2.setMoney(-money);
		var r2 = new BKuafuResult.Data();
		var future2 = super.callProcedureAsync("zeze2", "kuafu", a2, r2);

		m1 = future1.get().getMoney();
		m2 = future2.get().getMoney();
		logger.info("perform m1={} m2={}", m1, m2);

		return 0;
	}
}
