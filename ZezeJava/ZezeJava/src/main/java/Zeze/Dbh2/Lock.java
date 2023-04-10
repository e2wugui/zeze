package Zeze.Dbh2;

import Zeze.Net.Binary;

public class Lock {

	// 超时将抛出异常。
	public void lock() {
	}

	public void unlock() {

	}

	public static Lock get(Binary key) {
		return new Lock();
	}
}
