package Zeze.Dbh2;

import Zeze.Net.Binary;

public final class Locks extends Zeze.Util.Locks<Lockey> {
	public Lockey get(Binary tableKey) {
		return get(new Lockey(tableKey));
	}

	public boolean contains(Binary tableKey) {
		return contains(new Lockey(tableKey));
	}
}
