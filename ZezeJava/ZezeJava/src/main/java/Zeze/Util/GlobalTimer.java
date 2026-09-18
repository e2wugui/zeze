package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import org.jetbrains.annotations.NotNull;

public final class GlobalTimer {
	// 当前的秒级时间戳,由定时器更新,为了性能用opaque方式读写而不用volatile,多数CPU都能让所有线程及时看到最新值
	@SuppressWarnings("unused")
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

		// 两个 getter 同源（epoch），秒值由毫秒值派生（FND8-12）：原 curSec 用 nanoTime
		// 是任意原点的单调秒，与 getCurrentMillis 相差可达数年，同名族隐含的
		// getCurrentMillis()/1000 == getCurrentSeconds() 契约不成立；改用 epoch 放弃
		// 单调性，NTP回拨期间超时判定/滑窗清零延迟、可自愈，对秒级超时场景可忽略。
		vhCurMs.setOpaque(System.currentTimeMillis());
		vhCurSec.setOpaque(System.currentTimeMillis() / 1000);
		TaskSpec.ofAction(() -> {
			vhCurMs.setOpaque(System.currentTimeMillis());
			vhCurSec.setOpaque(System.currentTimeMillis() / 1000);
		}).schedulePeriodNow(1000, 1000);
	}

	/** epoch毫秒，由1秒周期任务采样缓存（精度只有1秒）。 */
	public static long getCurrentMillis() {
		return (long)vhCurMs.getOpaque();
	}

	/** epoch秒，与getCurrentMillis()同源同周期采样，getCurrentMillis()/1000与
	 * 本值相差0~1秒属正常（采样周期所致）。 */
	public static long getCurrentSeconds() {
		return (long)vhCurSec.getOpaque();
	}

	private GlobalTimer() {
	}
}
