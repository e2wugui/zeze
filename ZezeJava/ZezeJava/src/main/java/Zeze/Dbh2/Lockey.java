package Zeze.Dbh2;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public class Lockey implements Comparable<Lockey>{

	private final TableKey tableKey;
	// 超时将抛出异常。
	private Semaphore semaphore;
	private boolean locked = false;

	public Lockey(TableKey tableKey) {
		this.tableKey = tableKey;
	}

	public TableKey getTableKey() {
		return tableKey;
	}

	public Lockey alloc() {
		this.semaphore = new Semaphore(1);
		return this;
	}

	public void lock(Dbh2 dbh2) throws InterruptedException {
		if (!semaphore.tryAcquire(dbh2.getRaft().getRaftConfig().getAgentTimeout() * 2L, TimeUnit.MILLISECONDS))
			throw new RuntimeException("lock timeout");
		locked = true; // 只会有一个成功。
	}

	public void unlock() {
		if (locked)
			semaphore.release();
	}

	@Override
	public int compareTo(@NotNull Lockey o) {
		return tableKey.compareTo(o.tableKey);
	}
}
