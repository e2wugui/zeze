package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/** 全局低精度时钟：后台任务每秒采样 epoch 时间缓存，热路径 opaque 读免系统调用。 */
public final class GlobalTimer {
	// 当前时间戳由定时器每秒更新，为性能使用 opaque 读写而非 volatile
	@SuppressWarnings("unused")
	private static long curMs, curSec;
	private static final VarHandle vhCurMs, vhCurSec;

	static {
		try {
			var lookup = MethodHandles.lookup();
			vhCurMs = lookup.findStaticVarHandle(GlobalTimer.class, "curMs", long.class);
			vhCurSec = lookup.findStaticVarHandle(GlobalTimer.class, "curSec", long.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}

		// 秒值与毫秒值同源（epoch），保证 getCurrentMillis()/1000 与 getCurrentSeconds() 一致。
		refresh();
		TaskSpec.ofAction(GlobalTimer::refresh).schedulePeriodNow(1000, 1000);
	}

	private static void refresh() {
		var now = System.currentTimeMillis();
		vhCurMs.setOpaque(now);
		vhCurSec.setOpaque(now / 1000);
	}

	/** epoch 毫秒，由 1 秒周期任务采样缓存（精度 1 秒）。 */
	public static long getCurrentMillis() {
		return (long)vhCurMs.getOpaque();
	}

	/** epoch 秒，与 {@link #getCurrentMillis()} 同源同周期采样（精度 1 秒）。 */
	public static long getCurrentSeconds() {
		return (long)vhCurSec.getOpaque();
	}

	private GlobalTimer() {
	}
}
