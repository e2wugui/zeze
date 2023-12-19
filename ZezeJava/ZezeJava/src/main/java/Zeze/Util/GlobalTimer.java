package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import org.jetbrains.annotations.NotNull;

public final class GlobalTimer {
	// 当前的秒级时间戳,由定时器更新,为了性能用opaque方式读写而不用volatile,多数CPU都能让所有线程及时看到最新值
	@SuppressWarnings({"FieldMayBeFinal", "CanBeFinal", "unused"})
	private static long curMs, curSec;
	private static final @NotNull VarHandle vhCurMs, vhCurSec;

	static {
		try {
			var lookup = MethodHandles.lookup();
			vhCurMs = lookup.findStaticVarHandle(GlobalTimer.class, "curMs", long.class);
			vhCurSec = lookup.findStaticVarHandle(GlobalTimer.class, "curSec", long.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}

		vhCurMs.setOpaque(System.currentTimeMillis());
		vhCurSec.setOpaque(System.nanoTime() / 1_000_000_000);
		Task.scheduleUnsafe(1000, 1000, () -> {
			vhCurMs.setOpaque(System.currentTimeMillis());
			vhCurSec.setOpaque(System.nanoTime() / 1_000_000_000);
		});
	}

	// 精度只有1秒
	public static long getCurrentMillis() {
		return (long)vhCurMs.getOpaque();
	}

	public static long getCurrentSeconds() {
		return (long)vhCurSec.getOpaque();
	}

	private GlobalTimer() {
	}
}
