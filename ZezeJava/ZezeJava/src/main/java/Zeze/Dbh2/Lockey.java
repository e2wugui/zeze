package Zeze.Dbh2;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import Zeze.Net.Binary;
import org.jspecify.annotations.Nullable;

/**
 * Dbh2 事务的记录键锁，serialize 模式下基于信号量实现单键互斥。
 */
public class Lockey implements Zeze.Util.Lockey<Lockey>{

	private final Binary key;
	// 超时将抛出异常。
	private @Nullable Semaphore semaphore;
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
			//noinspection DataFlowIssue
			if (!semaphore.tryAcquire(0, TimeUnit.NANOSECONDS))
				throw new RuntimeException("lock timeout");
			locked = true; // 只会有一个成功。
		}
	}

	public void unlock() {
		if (locked) {
			// 须先复位 locked 再 release，保证 happens-before 且防止重复 unlock 增发 permit
			locked = false;
			//noinspection DataFlowIssue
			semaphore.release();
		}
	}

	@Override
	public int compareTo(Lockey o) {
		return key.compareTo(o.key);
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj)
			return true;
		return obj instanceof Lockey && key.equals(((Lockey)obj).key);
	}
}
