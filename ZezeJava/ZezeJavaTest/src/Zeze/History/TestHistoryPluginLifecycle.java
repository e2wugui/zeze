package Zeze.History;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import Zeze.Application;
import Zeze.Config;
import Zeze.Netty.HttpServer;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Procedure;
import Zeze.Util.DaemonTimer;
import Zeze.Util.Id128;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestHistoryPluginLifecycle {
	private static Object field(Object owner, String name) throws Exception {
		var field = owner.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(owner);
	}

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServerId(FastServerIds.takeoverPoolNext());
		conf.setServiceManager("disable");
		conf.setTakeoverMode("off");
		conf.setCheckpointMode(CheckpointMode.Table);
		conf.setHistory("HistoryPluginLifecycle");
		conf.setDefaultTableConf(new Config.TableConf());
		var db = new Config.DatabaseConf();
		db.setDatabaseUrl("history_plugin_lifecycle_" + conf.getServerId());
		conf.getDatabaseConfMap().put("", db);
		return new Application("HistoryPluginLifecycle", conf);
	}

	@Test
	public void readsAndCloseDoNotCreateLazyResources() throws Exception {
		var app = newApp();
		var module = HistoryModule.get(app);
		var table = new demo.Module1.Table3();
		app.addTable("", table);
		try {
			app.start();
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				assertNull(table.get(1L));
				return 0;
			}, "historyPureRead").call());
			assertNull(field(module, "pendingGidLedger"), "纯读不得取号或创建账本");
			assertNull(field(module, "applyHelper"), "启用采集不创建回放库");
		} finally {
			app.stop();
		}
		assertNull(field(module, "pendingGidLedger"), "关闭不得触发惰性创建");
		assertNull(field(module, "applyHelper"));
		assertNull(app.getHistory());
		assertThrows(IllegalStateException.class, () -> module.apply(1));
		assertThrows(IllegalStateException.class, module::getPendingGidLedger);
		module.close();
	}

	@Test
	public void replayCreatedOnFirstUseAndCannotResurrectAfterClose() throws Exception {
		var app = newApp();
		var module = HistoryModule.get(app);
		try {
			app.start();
			assertNull(field(module, "applyHelper"));
			module.apply(1);
			assertNotNull(field(module, "applyHelper"));
			assertNull(field(module, "pendingGidLedger"));
		} finally {
			app.stop();
		}
		assertNull(field(module, "applyHelper"));
		assertThrows(IllegalStateException.class, () -> module.apply(1));
	}

	@Test
	public void closeReleasesModuleLockAndContinuesAfterHttpFailure() throws Exception {
		var app = newApp();
		var module = HistoryModule.get(app);
		var ledger = module.getPendingGidLedger();
		ledger.register(new Id128(0, 1), System.nanoTime());
		var attempts = new java.util.concurrent.atomic.AtomicInteger();
		var http = new HttpServer() {
			@Override
			public void close() {
				// 模拟 close 等待一个必须先获得模块锁的在飞 handler。
				CompletableFuture.runAsync(() -> {
					module.lock();
					module.unlock();
				}).orTimeout(5, TimeUnit.SECONDS).join();
				if (attempts.getAndIncrement() == 0)
					throw new IllegalStateException("injected HTTP close failure");
				super.close();
			}
		};
		var httpField = HistoryModule.class.getDeclaredField("httpServer");
		httpField.setAccessible(true);
		httpField.set(module, http);
		try {
			assertThrows(IllegalStateException.class, module::close);
			assertTrue(((DaemonTimer)field(ledger, "sweepDaemon")).isShutdown(),
					"HTTP 清理失败不能跳过账本清理");
			assertNull(app.getTable(module.getHistoryTable().getName()), "仍须注销历史表");
			module.close(); // 重试上次未完成的 HTTP 清理
			module.close();
			assertNull(field(module, "httpServer"));
		} finally {
			module.close();
			app.stop();
		}
	}

	@Test
	public void initializedLedgerDoesNotWaitForReplayLock() throws Exception {
		var app = newApp();
		var module = HistoryModule.get(app);
		var ledger = module.getPendingGidLedger();
		try {
			module.lock(); // 模拟在飞回放持有模块锁，普通提交只需读取已初始化的账本。
			try {
				assertSame(ledger, CompletableFuture.supplyAsync(module::getPendingGidLedger)
						.get(5, TimeUnit.SECONDS));
			} finally {
				module.unlock();
			}
		} finally {
			module.close();
			app.stop();
		}
	}

	@Test
	public void pendingHistoryRejectsDifferentOwnerOrTable() {
		var table = new Zeze.Builtin.HistoryModule.tHistory();
		var ledger = new PendingGidLedger("first");
		var data = new Zeze.Builtin.HistoryModule.BLogChanges.Data();
		data.setGlobalSerialId(new Id128(0, 1));
		var first = new History(table, ledger, data);
		assertThrows(IllegalArgumentException.class,
				() -> first.mergeFrom(new History(table, new PendingGidLedger("other"), data)));
		assertThrows(IllegalArgumentException.class,
				() -> first.mergeFrom(new History(new Zeze.Builtin.HistoryModule.tHistory(), ledger, data)));
		assertSame(table, first.snapshot().getTable());
	}
}
