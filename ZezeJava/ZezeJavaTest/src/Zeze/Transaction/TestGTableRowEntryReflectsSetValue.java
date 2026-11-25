package Zeze.Transaction;

import java.util.ConcurrentModificationException;

import Zeze.Application;
import Zeze.Config;
import Zeze.History.Helper;
import Zeze.Transaction.GTable.GTable1;
import demo.ModuleGTable.tGTable;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 行视图entry的setValue语义（Map.Entry契约）：写入成功后，本entry的
 * getValue/equals/hashCode必须反映新值——delegate是pcollections不可变entry，
 * 转发读它的值会让调用方刚写入的entry读到旧值（随后据此比较、序列化或
 * 继续处理得到旧数据）。实际表数据经Row.put正确更新，仅entry视图读旧。
 */
@Fast
public class TestGTableRowEntryReflectsSetValue {

	@Test
	public void rowEntryReflectsOwnSetValue() {
		var table = new GTable1<Integer, Integer, Integer>(Integer.class, Integer.class, Integer.class);
		table.put(1, 2, 3);
		var entry = table.row(1).entrySet().iterator().next();

		assertEquals(3, entry.getValue());
		var old = entry.setValue(4);

		assertEquals(3, old, "setValue返回旧值");
		assertEquals(4, entry.getValue(), "setValue成功后entry必须读到新值（修复前仍读delegate旧值3）");
		assertEquals(4, table.get(1, 2), "实际表数据正确更新");

		var entryB = table.row(1).entrySet().iterator().next();
		assertEquals(entry, entryB, "同键同值的entry相等（equals经getValue读新值）");
		assertEquals(entry.hashCode(), entryB.hashCode(), "hashCode与equals契约一致");

		entry.setValue(5);
		assertNotEquals(entry, entryB, "值不同则不等（修复前equals比较恒读旧值，修改不可见）");
		assertEquals(5, table.get(1, 2));
	}

	@Test
	public void rowEntryIteratorRemoveAfterSetValue() {
		var table = new GTable1<Integer, Integer, Integer>(Integer.class, Integer.class, Integer.class);
		table.put(1, 2, 3);
		table.put(1, 5, 6);

		var it = table.row(1).entrySet().iterator();
		assertThrows(IllegalStateException.class, it::remove);

		var first = it.next();
		var firstCol = first.getKey();
		first.setValue(400); // 使用超出 Integer 缓存范围的值验证身份比对
		it.remove();
		assertNull(table.get(1, firstCol));
		assertEquals(1, table.row(1).size());
		assertTrue(table.containsRow(1));
		// 连续 remove 必须抛 IllegalStateException
		assertThrows(IllegalStateException.class, it::remove);

		// 删除最后一个单元格后必须维护空行摘除不变量
		var second = it.next();
		second.setValue(500);
		it.remove();
		assertFalse(table.containsRow(1));
		assertTrue(table.isEmpty());
	}

	@Test
	public void rowEntryIteratorRemoveAfterExternalOverwriteThrowsCme() {
		var table = new GTable1<Integer, Integer, Integer>(Integer.class, Integer.class, Integer.class);
		table.put(1, 2, 3);

		var it = table.row(1).entrySet().iterator();
		var entry = it.next();
		entry.setValue(4);
		// 外部覆盖同键后，再调 it.remove 仍必须 fail-fast 抛 CME
		table.put(1, 2, 99);
		assertThrows(ConcurrentModificationException.class, it::remove);
		assertEquals(99, table.get(1, 2));
	}

	@Test
	public void rowEntryIteratorRemoveAfterSetValueInTransaction() throws Exception {
		int id = FastServerIds.takeoverPoolNext();
		var config = new Config();
		config.setServerId(id);
		config.setServiceManager("disable");
		config.setDefaultTableConf(new Config.TableConf());
		var db = new Config.DatabaseConf();
		db.setDatabaseUrl("GTableRowEntrySetValueRemove_" + id);
		config.getDatabaseConfMap().put("", db);
		var app = new Application("GTableRowEntrySetValueRemove" + id, config);
		app.setSchemas(new demo.Schemas());
		app.addTable("", new tGTable());
		app.start();
		Helper.registerAllTableLogs(app);
		try {
			var table = (tGTable)requireNonNull(app.getTable("demo_ModuleGTable_tGTable"));
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				var v = table.getOrAdd(1L);
				v.getGTable().put(1, 2, 3);
				return Procedure.Success;
			}, "seed").call());

			// 事务内 setValue 后 it.remove 并回滚：原数据不受影响
			assertEquals(Procedure.LogicError, app.newProcedure(() -> {
				var g = requireNonNull(table.get(1L)).getGTable();
				var it = g.row(1).entrySet().iterator();
				var entry = it.next();
				entry.setValue(4);
				it.remove();
				assertFalse(g.containsRow(1));
				return Procedure.LogicError;
			}, "setValueAndRemoveRollback").call());

			assertEquals(Procedure.Success, app.newProcedure(() -> {
				var g = requireNonNull(table.get(1L)).getGTable();
				assertEquals(3, g.get(1, 2));
				// 事务内 setValue 后 it.remove 并提交：单元格与空行被正确摘除
				var it = g.row(1).entrySet().iterator();
				var entry = it.next();
				entry.setValue(4);
				it.remove();
				assertFalse(g.containsRow(1));
				return Procedure.Success;
			}, "setValueAndRemoveCommit").call());

			assertEquals(Procedure.Success, app.newProcedure(() -> {
				var g = requireNonNull(table.get(1L)).getGTable();
				assertFalse(g.containsRow(1));
				assertTrue(g.isEmpty());
				return Procedure.Success;
			}, "verifyCommitted").call());
		} finally {
			app.stop();
		}
	}
}
