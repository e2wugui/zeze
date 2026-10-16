package Zeze.Transaction;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Config;
import Zeze.History.HistoryModule;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestHistoryDisabled {

	private static Config newConf() {
		var conf = new Config();
		conf.setServerId(FastServerIds.takeoverPoolNext());
		conf.setServiceManager("disable");
		conf.setDefaultTableConf(new Config.TableConf());
		var db = new Config.DatabaseConf();
		db.setDatabaseUrl("history_disabled_" + conf.getServerId());
		conf.getDatabaseConfMap().put("", db);
		return conf;
	}

	private static @Nullable Object lazyResource(Application app, String name) throws Exception {
		var field = HistoryModule.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(HistoryModule.get(app));
	}

	/** 真实提交路径上的探针：日志仍须 commit，但无消费者时不应再遍历 category 收集变更。 */
	private static final class ProbeLog extends Log {
		int categoryReads;
		int commits;

		ProbeLog() {
			super(null, 0);
		}

		@Override
		public long getLogKey() {
			return Long.MIN_VALUE;
		}

		@Override
		public Category category() {
			categoryReads++;
			return Category.eUser;
		}

		@Override
		public int getTypeId() {
			return 0;
		}

		@Override
		public void commit() {
			commits++;
		}
	}

	@Test
	public void disabledTableMode() throws Exception {
		disabledSkipsHistoryAndPreservesChanges(CheckpointMode.Table);
	}

	@Test
	public void disabledImmediatelyMode() throws Exception {
		disabledSkipsHistoryAndPreservesChanges(CheckpointMode.Immediately);
	}

	private void disabledSkipsHistoryAndPreservesChanges(CheckpointMode mode) throws Exception {
		var conf = newConf();
		conf.setCheckpointMode(mode);
		conf.setCheckpointPeriod(3_600_000);
		var app = new Application("TestHistoryDisabled", conf);
		try {
			assertFalse(conf.isHistory());
			var table = new demo.Module1.Table3();
			app.addTable("", table);
			app.start();
			var probe = new ProbeLog();
			var callbacks = new AtomicInteger();
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				table.getOrAdd(1L).setLong2(10);
				var trans = Transaction.getCurrent();
				assertNull(app.getHistory(), "未启用时没有 History 插件");
				requireNonNull(trans).putLog(probe);
				Transaction.whileCommit(callbacks::incrementAndGet);
				return 0;
			}, "writeWithoutHistory").call());
			assertEquals(1, probe.commits);
			assertEquals(0, probe.categoryReads, "无 History、无监听器时不遍历日志收集变更");
			assertEquals(1, callbacks.get(), "提交回调仍执行");
			requireNonNull(app.getCheckpoint()).runOnce();
			assertEquals(10, requireNonNull(table.selectFromDatabase(1L)).getLong2(), "无历史模块时仍正常刷盘");

			var states = new ArrayList<Integer>();
			var values = new ArrayList<Long>();
			ChangeListener listener = (key, change) -> {
				states.add(change.getState());
				if (change.getState() != Changes.Record.Remove)
					values.add(requireNonNull(((demo.Module1.BValue)change.getValue())).getLong2());
			};
			table.getChangeListenerMap().addListener(listener);
			assertEquals(0, app.newProcedure(() -> {
				table.getOrAdd(2L).setLong2(20);
				assertNull(app.getHistory(), "监听通知不创建 History 插件");
				return 0;
			}, "listenerPut").call());
			assertEquals(0, app.newProcedure(() -> {
				requireNonNull(table.get(2L)).setLong2(21);
				return 0;
			}, "listenerEdit").call());
			assertEquals(0, app.newProcedure(() -> {
				table.remove(2L);
				return 0;
			}, "listenerRemove").call());
			assertEquals(java.util.List.of(Changes.Record.Put, Changes.Record.Edit, Changes.Record.Remove), states);
			assertEquals(java.util.List.of(20L, 21L), values);
			table.getChangeListenerMap().removeListener(listener);
			conf.setHistory("lateEnable");
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				table.getOrAdd(3L).setLong2(30);
				return 0;
			}, "configurationDoesNotEnableHistory").call(), "运行期修改配置不改变已装配能力");
			conf.setHistory("");
			assertEquals(0, app.newProcedure(() -> {
				assertEquals(30, requireNonNull(table.get(3L)).getLong2());
				return 0;
			}, "readAfterConfigurationChange").call());
			assertNull(app.getHistory(), "普通事务与监听通知均不创建历史模块或账本");
		} finally {
			app.stop();
		}
	}

	@Test
	public void disabledDoesNotInitializeHistory() throws Exception {
		var app = new Application("TestHistoryNotInitialized", newConf());
		try {
			assertNull(app.getHistory(), "默认不创建 HistoryModule");
			assertNull(app.getTable("Zeze_Builtin_HistoryModule_tHistory"), "默认不注册历史表");
			assertThrows(IllegalStateException.class, () -> HistoryModule.get(app));
		} finally {
			app.stop();
		}
	}

	@Test
	public void configuredHistoryRegistersItsTable() throws Exception {
		var conf = newConf();
		conf.setHistory("TestHistoryDisabled.enabled");
		var app = new Application("TestHistoryEnabled", conf);
		try {
			assertNotNull(app.getHistory());
			assertNull(lazyResource(app, "pendingGidLedger"));
			assertNull(lazyResource(app, "applyHelper"));
			assertSame(HistoryModule.get(app).getHistoryTable(),
					app.getTable("Zeze_Builtin_HistoryModule_tHistory"));
		} finally {
			requireNonNull(app.getHistory()).close();
			app.stop();
		}
	}
}
