package Zeze.Dbh2;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import Zeze.Net.Binary;
import org.jetbrains.annotations.NotNull;

public class Lockey implements Zeze.Util.Lockey<Lockey>{

	private final Binary key;
	// 超时将抛出异常。
	private Semaphore semaphore;
	private boolean locked = false;

	public Lockey(Binary key) {
		this.key = key;
	}

	public Binary getKey() {
		return key;
	}

	@Override
	public Lockey alloc() {
		this.semaphore = new Semaphore(1);
		return this;
	}

	public void lock(Dbh2 dbh2) throws InterruptedException {
		if (dbh2.getDbh2Config().isSerialize()) {
			if (!semaphore.tryAcquire(0, TimeUnit.NANOSECONDS))
				throw new RuntimeException("lock timeout");
			locked = true; // 只会有一个成功。
		}
	}

	public void unlock() {
		if (locked)
			semaphore.release();
	}

	@Override
	public int compareTo(@NotNull Lockey o) {
		return key.compareTo(o.key);
	}
}
