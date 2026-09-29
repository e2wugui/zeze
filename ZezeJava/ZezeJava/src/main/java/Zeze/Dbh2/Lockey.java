package Zeze.Dbh2;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import Zeze.Net.Binary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dbh2 事务的记录键锁，serialize 模式下基于信号量实现单键互斥。
 */
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
		if (locked) {
			// 复位先于release：后续获取方经信号量的happens-before观察到复位后的false，
			// 其重置的true不会被本次写覆盖（release在前会与下一获取方交错丢true致泄漏）。
			// 复位后locked准确表达"当前持有"，误触的unlock不再凭空增发permit。
			locked = false;
			semaphore.release();
		}
	}

	@Override
	public int compareTo(@NotNull Lockey o) {
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
