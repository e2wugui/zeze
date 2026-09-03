package UnitTest.Zeze.Trans;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Transaction.IGlobalAgent;
import Zeze.Util.TaskSpec;
import demo.Module1.Table3;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND-T2-3 回归：reduceInvalidAllLocalOnly 的 durability-before-downgrade。
 * Releaser（GCM断连降级）先调用本方法把记录置Invalid，之后才执行全服checkpoint；
 * 若降级时对脏记录不flush，GCM可能已把权限授予其他进程，本进程稍后的checkpoint
 * 会用旧事务的脏值覆盖别人的修改（跨进程丢更新）。
 * 表用demo的持久表Table3：Application构造会自动注册Builtin组件表
 * （tQueues等），addTable同名表会报duplicate table id。
 */
@Fast
public class TestReduceInvalidAllFlush {
	// 独立serverId隔离本地zeze_cache目录与其他@Fast测试（T2-1组300、T2-2组400起）。
	private static final AtomicInteger nextServerId = new AtomicInteger(500);

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(nextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("t2_reduceinvalid_test_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestReduceInvalidAllFlush@" + conf.getServerId(), conf);
	}

	/** 最小stub：reduceInvalidAllLocalOnly只使用hashIndex路由。 */
	private static final IGlobalAgent stubAgent = new IGlobalAgent() {
		@Override
		public @Nullable AcquireResult acquire(@NotNull Binary gkey, int state, boolean fresh, boolean noWait) {
			return AcquireResult.getSuccessResult(state);
		}

		@Override
		public int getGlobalCacheManagerHashIndex(@NotNull Binary gkey) {
			return 0;
		}

		@Override
		public @NotNull Zeze.Transaction.GlobalAgentBase getAgent(int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getAgentCount() {
			return 1;
		}

		@Override
		public void close() {
		}
	};

	private static void setGlobalAgent(Application app, IGlobalAgent agent) throws ReflectiveOperationException {
		Field f = Application.class.getDeclaredField("globalAgent");
		f.setAccessible(true);
		f.set(app, agent);
	}

	private static void reduceInvalidAllLocalOnly(Table3 table) throws ReflectiveOperationException {
		// 方法由TableX的final实现提供（生成类不覆写），需沿类层次查找。
		for (Class<?> c = table.getClass(); c != null; c = c.getSuperclass()) {
			for (Method m : c.getDeclaredMethods()) {
				if (m.getName().equals("reduceInvalidAllLocalOnly") && m.getParameterCount() == 1) {
					m.setAccessible(true);
					m.invoke(table, 0);
					return;
				}
			}
		}
		throw new NoSuchMethodException("reduceInvalidAllLocalOnly");
	}

	@Test
	public void testFlushDirtyBeforeInvalidate() throws Exception {
		var app = newApp();
		var table = new Table3();
		app.addTable("", table);
		app.start();
		try {
			setGlobalAgent(app, stubAgent);
			var key = 21L;
			var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
				table.getOrAdd(key).setInt_1(200);
				return 0L;
			}, "TestReduceInvalidAllFlush.put")).call();
			Assertions.assertEquals(0L, rc);
			// 此刻：记录dirty=true（未checkpoint），后台库中无该记录。

			// Releaser第一步：本地降级（不通知GCM）。
			reduceInvalidAllLocalOnly(table);

			// 核心：降级完成时脏数据必须已经落库
			//（此后Releaser才会执行全服checkpoint；期间GCM可能已授权其他进程）。
			var fromDb = table.selectFromDatabase(key);
			Assertions.assertNotNull(fromDb, "降级前必须flush脏记录，后台库中找不到该记录");
			Assertions.assertEquals(200, fromDb.getInt_1());

			// 事务路径仍能重新装载（Invalid后重新acquire）。
			int[] out = {Integer.MIN_VALUE};
			var rcGet = TaskSpec.ofProcedure(app.newProcedure(() -> {
				var v = table.get(key);
				out[0] = v != null ? v.getInt_1() : Integer.MIN_VALUE;
				return 0L;
			}, "TestReduceInvalidAllFlush.get")).call();
			Assertions.assertEquals(0L, rcGet);
			Assertions.assertEquals(200, out[0]);
		} finally {
			app.stop();
		}
	}
}
