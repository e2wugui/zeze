package Zeze.Arch;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.Util.Task;
import harness.Fast;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-90回归：LoadBase.onTimerTask的自续链重排曾位于各分支尾部、无try/finally——
 * 方法体任何一点抛运行时异常（抽象计数器实现抛错、report网络路径抛错、
 * maxOnlineNew配0时128行除零）重排即被跳过，ofAction吞异常记一条日志后链永久断，
 * 负载上报静默停止到进程重启（消费端用冻结快照做分配决策）。修复：对齐
 * Online.verifyLocal的try/finally形态——各分支只决定下一次延迟，重排统一收口到
 * finally（异常兜底默认消化延迟）；除法Math.max(1,maxOnlineNew)纵深防护；
 * setMaxOnlineNew拒绝<=0。孪生：ProviderOverload.ThreadPoolMonitor检测体
 * try/finally续链+提交失败记error（曾为吞噬进Future的零日志静默断链）。
 */
@Fast
public class TestFnd890LoadBaseChainResilience {

	// a6专属serverId段（不start不建缓存目录，防御性错开）。
	private static final AtomicInteger NextServerId = new AtomicInteger(16211);

	private static Application newApp(String name) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("a6_fnd890_" + conf.getServerId()); // 构造期模块注册需要，不start不真用
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application(name, conf);
	}

	/** 计数与抛错可控的轻量LoadBase：report覆写为空，不触网络。 */
	private static final class TestLoad extends LoadBase {
		final AtomicInteger ticks = new AtomicInteger();
		final AtomicBoolean throwOnce = new AtomicBoolean();
		final LoadConfig config = new LoadConfig();

		TestLoad(@NotNull Application zeze) {
			super(zeze);
		}

		@Override
		public int getOnlineLocalCount() {
			if (throwOnce.compareAndSet(true, false))
				throw new RuntimeException("a6 fnd890 boom");
			return ticks.incrementAndGet();
		}

		@Override
		public long getOnlineLoginTimes() {
			return ticks.get(); // 每拍+1 → onlineNewPerSecond>=1
		}

		@Override
		public LoadConfig getLoadConfig() {
			return config;
		}

		@Override
		public String getServiceIp() {
			return "127.0.0.1";
		}

		@Override
		public int getServicePort() {
			return 0;
		}

		@Override
		public void report(int overload, int online, int onlineNew) {
			// 不触ServiceManager/LoginQueue网络。
		}
	}

	private static void awaitTicks(String what, TestLoad load, int expect) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 10_000;
		while (load.ticks.get() < expect) {
			if (System.currentTimeMillis() > deadline)
				Assertions.fail("timeout waiting ticks>=" + expect + " (" + what + "): " + load.ticks.get());
			//noinspection BusyWait
			Thread.sleep(20);
		}
	}

	/** 主回归：某拍抛运行时异常后链条必须继续（finally统一重排），不再永久断链。 */
	@Test
	public void testChainSurvivesTickException() throws Exception {
		Task.tryInitThreadPool();
		var load = new TestLoad(newApp("TestFnd890Chain1"));
		load.start(1);
		try {
			awaitTicks("first tick", load, 1);
			load.throwOnce.set(true); // 下一拍getOnlineLocalCount抛RuntimeException
			awaitTicks("chain survives exception", load, 3); // 修复前：断链，ticks停在异常前
		} finally {
			load.stop();
		}
	}

	/**
	 * 加固一：maxOnlineNew=0（反射模拟旧对象/XML直配持有非法值）且onlineNewPerSecond>0时，
	 * 消化延迟除法不得除零断链（Math.max(1,..)纵深防护）。
	 */
	@Test
	public void testZeroMaxOnlineNewDivGuard() throws Exception {
		Task.tryInitThreadPool();
		var load = new TestLoad(newApp("TestFnd890Chain2"));
		var field = LoadConfig.class.getDeclaredField("maxOnlineNew");
		field.setAccessible(true);
		field.set(load.config, 0); // 绕过setter，模拟校验前遗留的非法配置
		load.start(1);
		try {
			awaitTicks("zero maxOnlineNew no divide-by-zero", load, 3); // 修复前：ArithmeticException断链
		} finally {
			load.stop();
		}
	}

	/** 加固二：setMaxOnlineNew拒绝<=0（主源码零调用方，无兼容破坏）。 */
	@Test
	public void testMaxOnlineNewSetterRejectsNonPositive() {
		var config = new LoadConfig();
		Assertions.assertThrows(IllegalArgumentException.class, () -> config.setMaxOnlineNew(0));
		Assertions.assertThrows(IllegalArgumentException.class, () -> config.setMaxOnlineNew(-1));
		Assertions.assertDoesNotThrow(() -> config.setMaxOnlineNew(1));
	}

	/** 停机语义护栏：stop后链条不再重排（finally的stopped检查不复活链条）。 */
	@Test
	public void testStopStillTerminatesChain() throws Exception {
		Task.tryInitThreadPool();
		var load = new TestLoad(newApp("TestFnd890Chain3"));
		load.start(1);
		try {
			awaitTicks("first tick", load, 1);
		} finally {
			load.stop();
		}
		Thread.sleep(2_500); // 覆盖至少两个调度周期（digestion默认1s）
		var ticksAtStop = load.ticks.get();
		Thread.sleep(1_500);
		Assertions.assertEquals(ticksAtStop, load.ticks.get(), "stop后链条必须终止（不复活）");
	}

	/** 孪生：ThreadPoolMonitor提交失败（池已关闭）留下error痕迹（曾零日志静默断链）。 */
	@Test
	public void testOverloadMonitorSubmitFailLogged() throws Exception {
		Task.tryInitThreadPool();
		var messages = new CopyOnWriteArrayList<String>();
		var appender = new AbstractAppender("a6_fnd890_capture", null, null, true, Property.EMPTY_ARRAY) {
			@Override
			public void append(@NotNull LogEvent event) {
				if (event.getLevel() == Level.ERROR)
					messages.add(event.getMessage().getFormattedMessage());
			}
		};
		appender.start();
		var log = (Logger)LogManager.getLogger(ProviderOverload.class);
		log.addAppender(appender);
		var providerOverload = new ProviderOverload();
		try {
			var closedPool = new AbstractExecutorService() {
				@Override
				public void shutdown() {
				}

				@Override
				public @NotNull List<Runnable> shutdownNow() {
					return List.of();
				}

				@Override
				public boolean isShutdown() {
					return true;
				}

				@Override
				public boolean isTerminated() {
					return true;
				}

				@Override
				public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) {
					return true;
				}

				@Override
				public void execute(@NotNull Runnable command) {
					throw new RejectedExecutionException("a6 pool closed");
				}
			};
			Assertions.assertTrue(providerOverload.register(closedPool, new Config()),
					"注册监控必须成功（构造即排首拍，延迟1~2秒）");
			long deadline = System.currentTimeMillis() + 10_000;
			while (messages.stream().noneMatch(m -> m.contains("submit fail"))) {
				if (System.currentTimeMillis() > deadline)
					Assertions.fail("提交失败必须记error（修复前零日志静默断链）");
				//noinspection BusyWait
				Thread.sleep(20);
			}
		} finally {
			providerOverload.close();
			log.removeAppender(appender);
		}
	}
}
