package Zeze.Util;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;
import com.sun.management.OperatingSystemMXBean;

/** JVM进程与系统平台指标：CPU、直接内存（java.nio.Bits反射）、物理内存/交换区。 */
public final class PlatformMetrics {
	public static final OperatingSystemMXBean osBean = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
	public static final Field fMaxDirectMemory; // long
	public static final AtomicLong reservedDirectMemory;
	public static final AtomicLong totalDirectCapacity;
	public static final AtomicLong directCount;

	static {
		try {
			var cBits = Class.forName("java.nio.Bits");
			fMaxDirectMemory = Json.setAccessible(cBits.getDeclaredField("MAX_MEMORY"));
			reservedDirectMemory = (AtomicLong)Json.setAccessible(cBits.getDeclaredField("RESERVED_MEMORY")).get(null);
			totalDirectCapacity = (AtomicLong)Json.setAccessible(cBits.getDeclaredField("TOTAL_CAPACITY")).get(null);
			directCount = (AtomicLong)Json.setAccessible(cBits.getDeclaredField("COUNT")).get(null);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private PlatformMetrics() {
	}

	public static long getMaxDirectMemory() {
		try {
			return fMaxDirectMemory.getLong(null);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		}
	}

	public static long getReservedDirectMemory() {
		return reservedDirectMemory.get();
	}

	public static long getTotalDirectCapacity() {
		return totalDirectCapacity.get();
	}

	public static long getDirectCount() {
		return directCount.get();
	}
}
