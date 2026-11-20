package Zeze.Transaction;


/** 记录锁池：按 TableKey 获取/复用 Lockey，同一锁池内的相同键共享锁。 */
public class Locks extends Zeze.Util.Locks<Lockey> {
	public Lockey get(TableKey tKey) {
		return super.get(new Lockey(tKey));
	}

	public boolean contains(TableKey tKey) {
		return super.contains(new Lockey(tKey));
	}
}
