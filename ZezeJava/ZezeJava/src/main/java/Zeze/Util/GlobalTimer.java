package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public final class GlobalTimer {
	// 当前的秒级时间戳,由定时器更新,为了性能用opaque方式读写而不用volatile,多数CPU都能让所有线程及时看到最新值
	@SuppressWarnings("FieldMayBeFinal")
	private static long curSec;
	private static final VarHandle vhCurSec;

	static {
		try {
			vhCurSec = MethodHandles.lookup().findStaticVarHandle(GlobalTimer.class, "curSec", long.class);
		} catch (ReflectiveOperationException e) {
			Task.forceThrow(e);
			throw new AssertionError(); // never run here
		}

		curSec = System.nanoTime() / 1_000_000_000;
		Task.scheduleUnsafe(1000, 1000, () -> vhCurSec.setOpaque(System.nanoTime() / 1_000_000_000));
	}

	public static long getCurrentSeconds() {
		return (long)vhCurSec.getOpaque();
	}

	private GlobalTimer() {
	}
}
