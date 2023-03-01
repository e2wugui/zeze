package Zeze.Dbh2;

public class Lock {
	public void lock() {

	}

	public void unlock() {

	}

	public static Lock get(byte[] key) {
		return new Lock();
	}
}
