package Zeze.Transaction;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/**
 * 同线程交替执行两份 Zeze；共享 RocksDB JNI，控制独立 fork 的原生提交偏移。
 * Memory 主库按类加载器隔离，仅测单实例刷盘，不模拟多 App/GCM 的同库部署。
 */
@NullMarked
public final class RecordFlushPair {
	private static final int OPERATIONS = 20_000;

	private static final class Isolated extends URLClassLoader {
		Isolated(URL[] urls) {
			super(urls, RecordFlushPair.class.getClassLoader());
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			synchronized (getClassLoadingLock(name)) {
				if (!name.startsWith("Zeze."))
					return super.loadClass(name, resolve);
				var type = findLoadedClass(name);
				if (type == null)
					type = findClass(name);
				if (resolve)
					resolveClass(type);
				return type;
			}
		}
	}

	private static final class Fixture implements AutoCloseable {
		final Isolated loader;
		final MethodHandle flush;
		final MethodHandle begin;
		final MethodHandle end;
		final MethodHandle stop;

		Fixture(String overlay, String classpath, int serverId, int valueSize)
				throws ReflectiveOperationException, MalformedURLException {
			var urls = new ArrayList<URL>();
			urls.add(new File(overlay).toURI().toURL());
			for (var entry : classpath.split(Pattern.quote(File.pathSeparator)))
				urls.add(new File(entry).toURI().toURL());
			loader = new Isolated(urls.toArray(URL[]::new));
			var type = loader.loadClass("Zeze.Transaction.RecordFlushBench");
			var instance = type.getConstructor().newInstance();
			type.getField("batchSize").setInt(instance, 1);
			type.getField("valueSize").setInt(instance, valueSize);
			var lookup = MethodHandles.publicLookup();
			var signature = MethodType.methodType(void.class);
			flush = lookup.findVirtual(type, "flush", signature).bindTo(instance);
			begin = lookup.findVirtual(type, "startDiagnostics", signature).bindTo(instance);
			end = lookup.findVirtual(type, "reportDiagnostics", signature).bindTo(instance);
			stop = lookup.findVirtual(type, "tearDown", signature).bindTo(instance);
			System.setProperty("recordFlush.serverId", Integer.toString(serverId));
			try {
				type.getMethod("setup").invoke(instance);
			} catch (ReflectiveOperationException | RuntimeException failure) {
				try {
					close();
				} catch (Exception cleanup) {
					failure.addSuppressed(cleanup);
				}
				throw failure;
			}
		}

		@Override
		public void close() throws Exception {
			try {
				stop.invokeExact();
			} catch (Throwable failure) {
				throw new RuntimeException("paired fixture cleanup", failure);
			} finally {
				loader.close();
			}
		}
	}

	private static long window(Fixture fixture, String side, int round) throws Throwable {
		fixture.begin.invokeExact();
		var bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
		long thread = Thread.currentThread().threadId();
		long bytes = bean.getThreadAllocatedBytes(thread);
		long start = System.nanoTime();
		for (int i = 0; i < OPERATIONS; i++)
			fixture.flush.invokeExact();
		long elapsed = System.nanoTime() - start;
		long allocated = bean.getThreadAllocatedBytes(thread) - bytes;
		System.out.printf("RECORD_FLUSH_PAIR round=%d side=%s ns=%.3f allocation=%.4f%n",
				round, side, elapsed / (double)OPERATIONS, allocated / (double)OPERATIONS);
		fixture.end.invokeExact();
		return elapsed;
	}

	public static void main(String[] args) throws Throwable {
		// 交换左右源码时，也交换初始化顺序和缓存目录，抵消实例位置偏移。
		int valueSize = Integer.parseInt(args[3]);
		try (var left = new Fixture(args[0], args[2], 10991, valueSize);
				var right = new Fixture(args[1], args[2], 10992, valueSize)) {
			long sumLeft = 0, sumRight = 0;
			for (int round = -2; round < 8; round++) {
				long l, r;
				if ((round & 1) == 0) {
					l = window(left, "L", round);
					r = window(right, "R", round);
					r += window(right, "R", round);
					l += window(left, "L", round);
				} else {
					r = window(right, "R", round);
					l = window(left, "L", round);
					l += window(left, "L", round);
					r += window(right, "R", round);
				}
				if (round >= 0) {
					sumLeft += l;
					sumRight += r;
				}
			}
			System.out.printf("RECORD_FLUSH_PAIR_SUMMARY leftNs=%.3f rightNs=%.3f%n",
					sumLeft / (16.0 * OPERATIONS), sumRight / (16.0 * OPERATIONS));
		} finally {
			System.clearProperty("recordFlush.serverId");
		}
	}
}
