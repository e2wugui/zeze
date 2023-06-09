package Zeze.Transaction;

public class Locks extends Zeze.Util.Locks<Lockey> {
	public Lockey get(TableKey tKey) {
		return super.get(new Lockey(tKey));
	}

	public boolean contains(TableKey tKey) {
		return super.contains(new Lockey(tKey));
	}
}
