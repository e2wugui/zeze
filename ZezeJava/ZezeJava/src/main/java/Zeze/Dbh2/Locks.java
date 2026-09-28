package Zeze.Dbh2;

import Zeze.Net.Binary;

/**
 * Dbh2 记录键锁（Lockey）的容器与查找入口。
 */
public final class Locks extends Zeze.Util.Locks<Lockey> {
	public Lockey get(Binary tableKey) {
		return get(new Lockey(tableKey));
	}

	public boolean contains(Binary tableKey) {
		return contains(new Lockey(tableKey));
	}
}
