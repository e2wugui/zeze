package Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Collections.LogMap1;
import Zeze.Transaction.Collections.LogSet1;
import Zeze.Transaction.Collections.LogSortedMap1;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * kimi-audit01 T-25回归：批量操作部分命中时把未变化元素也记入增量（幻影增量）。
 * LogSet1.addAll/removeAll先正确判定"是否真有变化"，但只要一项变化就把c全量记入
 * added/removed（set={a}时addAll([a,b])的a、removeAll([a,x])的x都是幻影）；
 * LogMap1/LogSortedMap1.putAll同构（值未变的键全量记入replaced）。
 * commit写回与followerApply幂等不受影响，但幻影增量进入Changes通知链，
 * 按增量驱动的下游监听器（推送、索引维护）会产生错误动作。
 * 测试：部分命中的批量操作后，日志增量必须只含真实变化（修复前含全量）。
 */
@Fast
public class TestSetMapPartialChangePhantomDelta {

	/** 托管PSet1路径：{1}上addAll([1,2])/removeAll([1,9])的增量只含真实变化。 */
	@Test
	public void testSetBulkPartialChangeDelta() throws Exception {
		var config = new Config();
		config.setServerId(FastServerIds.TEST_SET_MAP_PARTIAL_CHANGE_PHANTOM_DELTA);
		config.setServiceManager("disable");
		config.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.RocksDb);
		var dbDir = Files.createTempDirectory("partial_change_set");
		dbConf.setDatabaseUrl(dbDir.toString());
		config.getDatabaseConfMap().put("", dbConf);
		var app = new Application("TestT25SetPhantom", config);
		var table = new demo.Module1.tflush();
		app.addTable("", table);
		app.start();
		try {
			// 第一事务：落盘{1}（其日志随事务结束，第二事务的日志从零开始）
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				var set = table.getOrAdd(7082L).getSet10();
				set.clear();
				set.addAll(List.of(1));
				return Procedure.Success;
			}, "T25.Prepare").call());

			var result = app.newProcedure(() -> {
				var set = table.getOrAdd(7082L).getSet10();
				var logKey = set.parent().objectId() + set.variableId();
				Assertions.assertNull(Transaction.getCurrent().getLog(logKey), "第二事务在批量操作前不得有日志");

				// set={1}：addAll([1,2])中1已存在——真实新增只有2
				Assertions.assertTrue(set.addAll(List.of(1, 2)));
				var log = (LogSet1<Integer>)Transaction.getCurrent().getLog(logKey);
				Assertions.assertEquals(Set.of(2), log.getAdded(), "部分命中addAll的added不得含已存在元素（幻影add）");

				// set={1,2}：removeAll([1,9])中9不存在——真实移除只有1
				Assertions.assertTrue(set.removeAll(List.of(1, 9)));
				Assertions.assertEquals(Set.of(1), log.getRemoved(), "部分命中removeAll的removed不得含不存在元素（幻影remove）");
				return Procedure.Success;
			}, "T25.SetDelta").call();
			Assertions.assertEquals(Procedure.Success, result, "事务必须成功");
		} finally {
			app.stop();
			Zeze.Raft.LogSequence.deleteDirectory(dbDir.toFile());
		}
	}

	/** 托管PMap1/PSortedMap1路径：putAll部分变化时replaced只含值真实变化的键。 */
	@Test
	public void testMapPutAllPartialChangeDelta() throws Exception {
		var config = new Config();
		config.setServerId(FastServerIds.TEST_SET_MAP_PARTIAL_CHANGE_PHANTOM_DELTA + 1);
		config.setServiceManager("disable");
		config.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.RocksDb);
		var dbDir = Files.createTempDirectory("partial_change_map");
		dbConf.setDatabaseUrl(dbDir.toString());
		config.getDatabaseConfMap().put("", dbConf);
		var app = new Application("TestT25MapPhantom", config);
		var table = new demo.Module1.tflush();
		app.addTable("", table);
		app.start();
		try {
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				var v = table.getOrAdd(7083L);
				v.getMap40().clear();
				v.getMap40().put(1, 100);
				v.getSortedmap1().clear();
				v.getSortedmap1().put(1, 100);
				return Procedure.Success;
			}, "T25.PrepareMap").call());

			var result = app.newProcedure(() -> {
				var map = table.getOrAdd(7083L).getMap40();
				var mapLogKey = map.parent().objectId() + map.variableId();
				Assertions.assertNull(Transaction.getCurrent().getLog(mapLogKey), "第二事务在putAll前不得有日志");

				// {1:100}上putAll({1:100, 2:200})：键1值未变——真实变化只有键2
				map.putAll(Map.of(1, 100, 2, 200));
				var log = (LogMap1<Integer, Integer>)Transaction.getCurrent().getLog(mapLogKey);
				Assertions.assertEquals(Map.of(2, 200), log.getReplaced(),
						"部分变化putAll的replaced不得含值未变的键（幻影replaced）");

				var smap = table.getOrAdd(7083L).getSortedmap1();
				var smapLogKey = smap.parent().objectId() + smap.variableId();
				smap.putAll(Map.of(1, 100, 3, 300));
				var slog = (LogSortedMap1<Integer, Integer>)Transaction.getCurrent().getLog(smapLogKey);
				Assertions.assertEquals(Map.of(3, 300), slog.getReplaced(),
						"LogSortedMap1.putAll同构：replaced不得含值未变的键");
				return Procedure.Success;
			}, "T25.MapDelta").call();
			Assertions.assertEquals(Procedure.Success, result, "事务必须成功");
		} finally {
			app.stop();
			Zeze.Raft.LogSequence.deleteDirectory(dbDir.toFile());
		}
	}
}
