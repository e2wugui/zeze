package Zeze.History;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Application;
import Zeze.Arch.Online;
import Zeze.Builtin.HistoryModule.BLogChanges;
import Zeze.Builtin.Online.BAny;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.IHistory;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Table;
import Zeze.Transaction.TableDynamic;
import Zeze.Util.Id128;
import dynamicred.Demo.Amount;
import dynamicred.Demo.Line;
import dynamicred.Demo.Order;
import dynamicred.Demo.Quantity;
import harness.Fast;
import harness.FastServerIds;
import harness.proc.Procs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestDynamicListHistoryReplay {
	@Test
	public void restoresQuantityUsingItsFieldFactory(@TempDir Path dir) throws Exception {
		runProbe(dir, Probe.class);
	}

	@Test
	public void restoresNestedAndGTableChanges(@TempDir Path dir) throws Exception {
		runProbe(dir, Probe.class, "extended");
	}

	@Test
	public void restoresRuntimeTypeInSeparateProcess(@TempDir Path dir) throws Exception {
		runProbe(dir, RuntimeTypeProbe.class, "write");
		runProbe(dir, RuntimeTypeProbe.class, "replay");
	}

	private void runProbe(Path dir, Class<?> probe, String... probeArgs) throws Exception {
		// LogRegistry is process-global. Isolate both the fixture registration and the replay.
		var classpath = new LinkedHashSet<String>();
		for (var loader = getClass().getClassLoader(); loader != null; loader = loader.getParent()) {
			if (loader instanceof URLClassLoader urls) {
				for (var url : urls.getURLs()) {
					if (url.getProtocol().equals("file"))
						classpath.add(Path.of(url.toURI()).toAbsolutePath().toString());
				}
			}
		}
		for (var entry : System.getProperty("java.class.path").split(File.pathSeparator))
			classpath.add(Path.of(entry).toAbsolutePath().toString());

		var args = new ArrayList<>(List.of("-ea", "-cp", String.join(File.pathSeparator, classpath), probe.getName(),
				Integer.toString(FastServerIds.takeoverPoolNext())));
		args.addAll(List.of(probeArgs));
		var argsFile = dir.resolve("java.args");
		Files.write(argsFile, args.stream().map(arg -> '"' + arg.replace("\\", "\\\\").replace("\"", "\\\"") + '"').toList(),
				Charset.forName(System.getProperty("native.encoding", "UTF-8")));
		var output = dir.resolve(probeArgs.length == 0 ? "probe.log" : probeArgs[0] + ".log");
		var child = new ProcessBuilder(Procs.JAVAW, "@" + argsFile)
				.directory(dir.toFile()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
		try {
			assertTrue(child.waitFor(45, TimeUnit.SECONDS), () -> "History replay timed out: " + output);
			assertEquals(0, child.exitValue(), Files.readString(output));
		} finally {
			if (child.isAlive())
				child.destroyForcibly().waitFor();
		}
	}

	private static Application newApp(String serverId) throws Exception {
		var conf = new Config();
		conf.setServerId(Integer.parseInt(serverId));
		conf.setServiceManager("disable");
		conf.setTakeoverMode("off");
		conf.setCheckpointMode(CheckpointMode.Immediately);
		conf.setDefaultTableConf(new Config.TableConf());
		conf.setHistory("DynamicListHistoryReplay");
		var db = new Config.DatabaseConf();
		db.setDatabaseUrl("dynamic_list_history_replay_" + conf.getServerId());
		conf.getDatabaseConfMap().put("", db);
		return new Application("DynamicListHistoryReplay", conf);
	}

	private static void installLocalHistoryIds(Application app, HistoryModule history) throws Exception {
		// Substitute only the external ID allocation. Keep capture and flush on the real path.
		var ids = new AtomicLong();
		var slot = Application.class.getDeclaredField("history");
		slot.setAccessible(true);
		slot.set(app, new IHistory() {
			@Override
			public void registerLogs(Iterable<Table> tables) throws Exception {
				history.registerLogs(tables);
			}

			@Override
			public void afterOpen() {
				history.afterOpen();
			}

			@Override
			public void close() {
				history.close();
			}

			@Override
			public PreparedCommit prepareCommit(Procedure procedure) {
				var gid = new Id128(0, ids.incrementAndGet());
				var ledger = history.getPendingGidLedger();
				ledger.register(gid, System.nanoTime());
				return changes -> new History(history.getHistoryTable(), ledger,
						History.buildLogChanges(gid, changes, null, null));
			}
		});
	}

	public static final class RuntimeTypeProbe {
		private static final Path historyFile = Path.of("history.bin");

		public static void main(String[] args) throws Exception {
			var writing = args[1].equals("write");
			var app = newApp(args[0]);
			var history = HistoryModule.get(app);
			try {
				if (writing) {
					installLocalHistoryIds(app, history);
					Online.register(Order.class);
				}
				app.start();
				var table = new TableDynamic<Long, BAny>(app, "runtime_dynamic_history", key -> {
					var bb = ByteBuffer.Allocate();
					bb.WriteLong(key);
					return bb;
				}, ByteBuffer::ReadLong, BAny::new, false, null, Long.class, BAny.class);
				if (!writing)
					Online.register(Order.class);
				if (writing)
					writeHistory(app, history, table);
				else
					replayHistory(app, history, table);
			} finally {
				app.stop();
			}
		}

		private static void writeHistory(Application app, HistoryModule history, TableDynamic<Long, BAny> table)
				throws Exception {
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				var row = new BAny();
				row.getAny().setBean(new Order());
				table.put(1L, row);
				return Procedure.Success;
			}, "InsertOrder").call());
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				var row = Objects.requireNonNull(table.get(1L));
				var order = assertInstanceOf(Order.class, row.getAny().getBean());
				order.getDirectQuantity().setBean(new Quantity(52));
				return Procedure.Success;
			}, "ReplaceDirectQuantity").call());

			var records = ByteBuffer.Allocate();
			var historyTable = history.getHistoryTable();
			assertEquals(2, historyTable.walkDatabaseRaw((key, row) -> {
				if (historyTable.decodeKey(ByteBuffer.Wrap(key)).equals(new Id128(0, 2))) {
					// History 的日志查看入口也须能解码，且此前没有应用过任何记录。
					var edit = new BLogChanges.Data();
					edit.decode(ByteBuffer.Wrap(row));
					Verify.toString(edit);
				}
				records.WriteBytes(key);
				records.WriteBytes(row);
				return true;
			}));
			Files.write(historyFile, records.Copy());
		}

		private static void replayHistory(Application app, HistoryModule history, TableDynamic<Long, BAny> table)
				throws Exception {
			var records = ByteBuffer.Wrap(Files.readAllBytes(historyFile));
			var historyTable = history.getHistoryTable();
			var db = app.getDatabase("");
			var storage = db.openTable(historyTable.getName(), historyTable.getId());
			try (var txn = db.beginTransaction()) {
				while (records.size() > 0)
					storage.replace(txn, records.ReadByteBuffer(), records.ReadByteBuffer());
				txn.commit();
			}
			var replay = new ApplyHelper(app, historyTable, new ApplyDatabaseMemory(), 0, 0);
			var applied = replay.apply(10);
			assertEquals(new Id128(0, 2), replay.getExclusiveStartKey());
			assertEquals(1, applied.size());
			@SuppressWarnings("unchecked")
			var target = (ApplyTable<Long, BAny>)applied.keySet().iterator().next();
			assertEquals(table, target.getOriginTable());
			var order = assertInstanceOf(Order.class, Objects.requireNonNull(target.get(1L)).getAny().getBean());
			assertEquals(52, assertInstanceOf(Quantity.class, order.getDirectQuantity().getBean()).getQuantity());
		}
	}

	public static final class Probe {
		public static final class InheritedOrder extends Order {
		}

		private static DynamicBean value(DynamicBean wrapper, Bean bean) {
			wrapper.setBean(bean);
			return wrapper;
		}

		private static void changeNestedValues(Application app, TableDynamic<Long, Order> orders) {
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				var order = Objects.requireNonNull(orders.get(1001L));
				var wrong = value(Order.newDynamicBean_QuantityGridSource(), new Quantity(10));
				assertThrows(IllegalArgumentException.class, () -> order.getQuantities().add(wrong));
				assertTrue(!wrong.isManaged());
				var unbound = new DynamicBean(0, Order::getSpecialTypeIdFromBean_8, Order::createBeanFromSpecialTypeId_8);
				assertThrows(IllegalArgumentException.class, () -> order.getQuantityMap().put(1, unbound));
				assertTrue(!unbound.isManaged());
				assertThrows(IllegalArgumentException.class, () -> order.getQuantityGrid().put(2, 1, wrong));
				assertTrue(!order.getQuantityGrid().containsRow(2));
				var targetRows = order.getQuantityGrid().getPMap2();
				var foreignRow = order.getAmountGrid().getPMap2().createValue();
				foreignRow.put(1, value(Order.newDynamicBean_AmountGrid(), new Amount(10)));
				assertThrows(IllegalArgumentException.class, () -> targetRows.put(2, foreignRow));
				var validRow = targetRows.createValue();
				validRow.put(1, value(Order.newDynamicBean_QuantityGrid(), new Quantity(10)));
				var rows = new LinkedHashMap<>(Map.of(2, validRow));
				rows.put(3, foreignRow);
				assertThrows(IllegalArgumentException.class, () -> targetRows.putAll(rows));
				assertTrue(targetRows.isEmpty());
				assertTrue(!validRow.isManaged());
				assertTrue(!foreignRow.isManaged());
				assertNull(validRow.mapKey());
				assertNull(foreignRow.mapKey());
				order.getLines().add(value(Order.newDynamicBean_Lines(), new Line()));
				order.getLines().add(value(Order.newDynamicBean_Lines(), new Line()));
				order.getAmountGrid().put(1, 1, value(Order.newDynamicBean_AmountGrid(), new Amount(30)));
				order.getQuantityGridSource().put(1, 1, value(Order.newDynamicBean_QuantityGridSource(), new Quantity(40)));
				order.getQuantityGrid().assign(order.getQuantityGridSource());
				return Procedure.Success;
			}, "InsertLinesAndGridRows").call());
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				var order = Objects.requireNonNull(orders.get(1001L));
				order.getLines().remove(0);
				var line = (Line)order.getLines().get(0).getBean();
				line.getAmounts().add(value(Line.newDynamicBean_Amounts(), new Amount(21)));
				assertThrows(UnsupportedOperationException.class,
						() -> order.getQuantityGrid().assign(order.getAmountGrid()));
				Objects.requireNonNull(order.getQuantityGrid().get(1, 1)).setBean(new Quantity(41));
				order.getQuantityGrid().put(1, 2, value(Order.newDynamicBean_QuantityGrid(), new Quantity(42)));
				order.getDirectAmount().setBean(new Amount(51));
				order.getDirectQuantity().setBean(new Quantity(52));
				order.getQuantityMap().put(1, value(Order.newDynamicBean_QuantityMap(), new Quantity(61)));
				order.getAmountSorted().put(1, value(Order.newDynamicBean_AmountSorted(), new Amount(62)));
				return Procedure.Success;
			}, "ChangeNestedValues").call());
		}

		private static void assertNestedValues(Order order) {
			assertEquals(1, order.getLines().size());
			var line = assertInstanceOf(Line.class, order.getLines().get(0).getBean());
			assertEquals(21, assertInstanceOf(Amount.class, line.getAmounts().get(0).getBean()).getAmount());
			assertEquals(30, assertInstanceOf(Amount.class, Objects.requireNonNull(order.getAmountGrid().get(1, 1)).getBean()).getAmount());
			assertEquals(41, assertInstanceOf(Quantity.class, Objects.requireNonNull(order.getQuantityGrid().get(1, 1)).getBean()).getQuantity());
			assertEquals(42, assertInstanceOf(Quantity.class, Objects.requireNonNull(order.getQuantityGrid().get(1, 2)).getBean()).getQuantity());
			var source = Objects.requireNonNull(order.getQuantityGridSource().get(1, 1));
			assertEquals(40, assertInstanceOf(Quantity.class, source.getBean()).getQuantity());
			assertEquals(51, assertInstanceOf(Amount.class, order.getDirectAmount().getBean()).getAmount());
			assertEquals(52, assertInstanceOf(Quantity.class, order.getDirectQuantity().getBean()).getQuantity());
			assertEquals(61, assertInstanceOf(Quantity.class, Objects.requireNonNull(order.getQuantityMap().get(1)).getBean()).getQuantity());
			assertEquals(62, assertInstanceOf(Amount.class, Objects.requireNonNull(order.getAmountSorted().get(1)).getBean()).getAmount());
		}

		public static void main(String[] args) throws Exception {
			var app = newApp(args[0]);
			var history = HistoryModule.get(app);
			try {
				installLocalHistoryIds(app, history);
				app.start();
				var orders = new TableDynamic<Long, Order>(app, "dynamic_list_history_orders", key -> {
					var bb = ByteBuffer.Allocate();
					bb.WriteLong(key);
					return bb;
				}, ByteBuffer::ReadLong, InheritedOrder::new, false, null, Long.class, InheritedOrder.class);
				assertEquals(Procedure.Success, app.newProcedure(() -> {
					orders.put(1001L, new InheritedOrder());
					return Procedure.Success;
				}, "InsertOrder").call());
				assertEquals(Procedure.Success, app.newProcedure(() -> {
					var order = Objects.requireNonNull(orders.get(1001L));
					var quantity = new Quantity();
					quantity.setQuantity(42);
					var item = Order.newDynamicBean_Quantities();
					item.setBean(quantity);
					order.getQuantities().add(item);
					return Procedure.Success;
				}, "AppendQuantity").call());

				boolean extended = args.length > 1;
				if (extended)
					changeNestedValues(app, orders);

				var replay = new ApplyHelper(app, history.getHistoryTable(), new ApplyDatabaseMemory(), 0, 0);
				var applied = replay.apply(10);
				assertEquals(new Id128(0, extended ? 4 : 2), replay.getExclusiveStartKey());
				assertEquals(1, applied.size());
				@SuppressWarnings("unchecked")
				var target = (ApplyTable<Long, Order>)applied.keySet().iterator().next();
				var order = Objects.requireNonNull(target.get(1001L));
				assertTrue(order.getAmounts().isEmpty());
				assertEquals(1, order.getQuantities().size());
				var item = order.getQuantities().get(0);
				assertEquals(7, item.getTypeId());
				var quantity = assertInstanceOf(Quantity.class, item.getBean(), "Order.Quantities[0]");
				assertEquals(42, quantity.getQuantity());
				if (extended)
					assertNestedValues(order);
			} finally {
				app.stop();
			}
		}
	}
}
