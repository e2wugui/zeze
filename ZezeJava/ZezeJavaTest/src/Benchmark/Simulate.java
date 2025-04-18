package Benchmark;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Util.Task;
import Zeze.Util.ThreadFactoryWithName;
import demo.Module1.Table5;
import demo.SimpleApp;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

@SuppressWarnings({"unused", "FieldCanBeLocal", "FieldMayBeFinal", "CanBeFinal"})
public class Simulate {
	static {
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(Level.INFO);
	}

	private static final Logger logger = LogManager.getLogger(Simulate.class);

	private static int serverId = 1;
	private static int taskThreadCount = 50;
	private static int schdThreadCount = 10;
	private static int totalKeyRange = 2_000_000;
	private static int localKeyRange = 100_000;
	private static int localKeyWindow = 10_000;
	private static int procsEveryWindowMove = 100; // 处理多少个事务后移动localKey的窗口
	private static int concurrentProcs = 100; // 执行事务的并发线程数
	private static int localPercent = 90; // localKey范围内取key的百分比
	private static String serviceManagerIp = "127.0.0.1";
	private static int serviceManagerPort = 5001;
	private static String globalServerIp = "127.0.0.1";
	private static int globalServerPort = 5002;

	private static int read1Weight = 40;
	private static int readWrite1Weight = 30;
	private static int read2Write1Weight = 20;
	private static int readWrite2Weight = 10;

	private static long keyBegin, keyEnd;
	private static final AtomicLong keyWindowBegin = new AtomicLong();
	private static Table5 table5;

	private static long randKey() {
		var rand = ThreadLocalRandom.current();
		if (rand.nextInt(100) >= localPercent)
			return rand.nextLong(totalKeyRange);
		var k = keyWindowBegin.get() + rand.nextInt(localKeyWindow);
		if (k >= keyEnd)
			k -= localKeyWindow;
		return k;
	}

	private static long read1() {
		var k = randKey();
		var r = table5.getOrAdd(k);
		r.getS();
		return 0;
	}

	private static long readWrite1() {
		var k = randKey();
		var r = table5.getOrAdd(k);
		r.setS(r.getS() + 1);
		return 0;
	}

	private static long read2Write1() {
		var k1 = randKey();
		var k2 = randKey();
		var r1 = table5.getOrAdd(k1);
		var r2 = table5.getOrAdd(k2);
		r2.setS(r1.getS() + r2.getS());
		return 0;
	}

	private static long readWrite2() {
		var k1 = randKey();
		var k2 = randKey();
		var r1 = table5.getOrAdd(k1);
		var r2 = table5.getOrAdd(k2);
		var v = r1.getS() + r2.getS();
		r1.setS(v);
		r2.setS(v);
		return 0;
	}

	private static final class Proc {
		final String name;
		final MethodHandle mh;
		final int weight;

		Proc(String name, MethodHandle mh, int weight) {
			this.name = name;
			this.mh = mh;
			this.weight = weight;
		}
	}

	private static final class ThreadFactory extends ThreadFactoryWithName {
		public ThreadFactory(String poolName) {
			super(poolName);
		}

		@Override
		public Thread newThread(Runnable r) {
			var t = super.newThread(r);
			t.setPriority(Thread.MIN_PRIORITY);
			return t;
		}
	}

	public static void main(String[] args) throws Exception {
		var lookup = MethodHandles.lookup();
		for (var arg : args) {
			var kv = arg.split("=");
			if (kv.length == 2) {
				var k = kv[0];
				if (k.endsWith("Ip"))
					lookup.findStaticVarHandle(Simulate.class, k, String.class).set(kv[1]);
				else {
					var v = Integer.parseInt(kv[1].replace("_", ""));
					if (v < 0 || v == 0 && !k.endsWith("Weight") && !k.equals("localPercent"))
						throw new IllegalArgumentException("invalid " + k + " = " + v);
					lookup.findStaticVarHandle(Simulate.class, k, int.class).set(v);
				}
			}
		}
		var procs = new ArrayList<Proc>();
		int totalWeight = 0;
		for (var f : Simulate.class.getDeclaredFields()) {
			if ((f.getType() == int.class || f.getType() == String.class) && Modifier.isStatic(f.getModifiers())) {
				var k = f.getName();
				Object v = lookup.unreflectVarHandle(f).get();
				logger.info("{} = {}", k, v);
				if (k.endsWith("Weight")) {
					totalWeight += (int)v;
					var name = k.substring(0, k.length() - 6);
					var mh = lookup.findStatic(Simulate.class, name, MethodType.methodType(long.class));
					procs.add(new Proc(name, mh, (int)v));
				}
			}
		}
		if (totalWeight <= 0)
			throw new IllegalArgumentException("invalid totalWeight = " + totalWeight);
		var totalWeight0 = totalWeight;
		keyBegin = (long)localKeyRange * serverId;
		keyEnd = keyBegin + localKeyRange;
		keyWindowBegin.set(keyBegin);

		Task.initThreadPool(Task.newFixedThreadPool(taskThreadCount, "ZezeTaskPool"),
				Executors.newScheduledThreadPool(schdThreadCount, new ThreadFactory("ZezeScheduledPool")));

		var app = new SimpleApp(serverId, 20000 + serverId + 1, 20000);
		app.getZeze().addTable("", table5 = new Table5());
		app.start();

		var totalCount = new AtomicLong();
		var lastTime = new AtomicLong(System.nanoTime());
		var state = new Object() {
			long lastCount;
		};
		for (int i = 0; i < concurrentProcs; i++) {
			var t = new Thread(() -> {
				try {
					for (; ; ) {
						var r = ThreadLocalRandom.current().nextInt(totalWeight0);
						Proc proc = null;
						for (var p : procs) {
							if ((r -= p.weight) < 0) {
								proc = p;
								break;
							}
						}
						var proc0 = proc;
						app.getZeze().newProcedure(() -> {
							try {
								return (long)proc0.mh.invokeExact();
							} catch (RuntimeException | Error e) {
								throw e;
							} catch (Throwable e) {
								throw new RuntimeException(e);
							}
						}, proc.name).call();

						var tc = totalCount.incrementAndGet();
						if (tc % procsEveryWindowMove == 0) {
							long v0, v1;
							do {
								v0 = keyWindowBegin.get();
								v1 = v0 + 1 < keyEnd ? v0 + 1 : keyBegin;
							} while (!keyWindowBegin.compareAndSet(v0, v1));
						}
						var t0 = lastTime.get();
						var t1 = System.nanoTime();
						var dt = t1 - t0;
						if (dt >= 1_000_000_000 && lastTime.compareAndSet(t0, t1)) {
							logger.info("finished {} {}/s", tc, (tc - state.lastCount) * 1_000_000_000L / dt);
							state.lastCount = tc;
						}
					}
				} catch (Throwable e) { // print stacktrace.
					logger.fatal("proc thread fatal exception:", e);
				}
			}, String.format("proc%04d", i));
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
		}
	}
}
