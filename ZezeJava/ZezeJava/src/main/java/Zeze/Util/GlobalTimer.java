package Zeze.Util;

public final class GlobalTimer {
	// 当前的秒级时间戳,由定时器更新,为了性能不用volatile,多数CPU都能让所有线程及时看到最新值
	private static long curSec = System.nanoTime() / 1_000_000_000;

	static {
		Task.scheduleUnsafe(1000, 1000, () -> curSec = System.nanoTime() / 1_000_000_000);
	}

	public static long getCurrentSeconds() {
		return curSec;
	}

	private GlobalTimer() {
	}
}
