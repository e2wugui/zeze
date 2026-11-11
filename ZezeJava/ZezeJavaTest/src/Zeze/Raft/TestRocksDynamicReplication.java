package Zeze.Raft;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import Zeze.Config;
import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.DynamicBean;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Raft.RocksRaft.Transaction;
import Zeze.Util.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jspecify.annotations.Nullable;
import rocksdynamic.Fixture.AbstractDynamicFixture;
import rocksdynamic.Fixture.Amount;
import rocksdynamic.Fixture.CustomPayload;
import rocksdynamic.Fixture.Quantity;
import rocksdynamic.Fixture.Value;

public class TestRocksDynamicReplication {
	@TempDir
	Path directory;

	@Test
	public void replaysDynamicChangesAcrossRestartLeaderSwitchAndSnapshotInstall() throws Exception {
		Task.tryInitThreadPool();
		var ports = freePorts();
		@Nullable Rocks[] nodes = new Rocks[ports.length];
		try {
			for (int i = 0; i < nodes.length; i++)
				start(ports, nodes, i);
			int leader = awaitLeader(nodes);
			var originalLeader = requireNonNull(nodes[leader]);
			long seed = write(originalLeader, value -> {
				value.getAmount().setBean(new Amount(1));
				value.getQuantity().setBean(new Quantity(2));
				value.getNested().getPayload().setBean(new Quantity(3));
				value.getNested().getSortedNumbers().put(1, 11L);
			});
			for (var node : nodes)
				awaitApplied(requireNonNull(node), seed);

			int offline = (leader + 1) % nodes.length;
			close(nodes, offline);
			write(originalLeader, value -> {
				var stale = payload(Amount.class, value.getAmount());
				var newAmount = new Amount(10);
				value.getAmount().setBean(newAmount);
				newAmount.getEntries().add(1);
				stale.getEntries().add(99);
				value.getQuantity().setBean(new Quantity(20));
				value.getNested().getPayload().setBean(new Quantity(21));
				value.getCustom().setBean(new CustomPayload());
				value.getNested().getSortedNumbers().put(2, 22L);

				var listItem = Value.newDynamicBean_Amounts();
				listItem.setBean(new Amount(30));
				value.getAmounts().add(listItem);
				var mapItem = Value.newDynamicBean_Quantities();
				mapItem.setBean(new Quantity(40));
				value.getQuantities().put(1, mapItem);
				var sortedItem = Value.newDynamicBean_SortedAmounts();
				sortedItem.setBean(new Amount(50));
				value.getSortedAmounts().put(1, sortedItem);
			});

			long edited = write(originalLeader, value -> {
				var amount = payload(Amount.class, value.getAmount());
				amount.getEntries().add(2);
				payload(CustomPayload.class, value.getCustom()).getEntries().add(100L);
				value.getNested().getSortedNumbers().remove(1);
				var txn = requireNonNull(Transaction.getCurrent());
				txn.begin();
				value.getAmount().setBean(new Quantity(99));
				txn.rollback();
				assertEquals(amount, value.getAmount().getBean());

				var listItem = value.getAmounts().get(0);
				listItem.setBean(new Amount(31));
				payload(Amount.class, listItem).getEntries().add(3);
				var temporary = Value.newDynamicBean_Amounts();
				temporary.setBean(new CustomPayload());
				value.getAmounts().add(temporary);
				value.getAmounts().remove(1);
				payload(Quantity.class, value.getQuantities().get(1)).setNumber(41);
				payload(Amount.class, value.getSortedAmounts().get(1)).setNumber(51);
			});

			start(ports, nodes, offline);
			for (var node : nodes) {
				var online = requireNonNull(node);
				awaitApplied(online, edited);
				var value = read(online);
				assertEquals(20, payload(Quantity.class, value.getQuantity()).getNumber());
				assertEquals(21, payload(Quantity.class, value.getNested().getPayload()).getNumber());
				assertEquals(71, value.getCustom().getSpecialTypeId());
				assertEquals(List.of(2), List.copyOf(value.getNested().getSortedNumbers().keys()));
				assertEquals(22L, requireNonNull(value.getNested().getSortedNumbers().get(2)).longValue());
				assertIterableEquals(List.of(100L), payload(CustomPayload.class, value.getCustom()).getEntries());
				var amount = payload(Amount.class, value.getAmount());
				assertEquals(10, amount.getNumber());
				assertIterableEquals(List.of(1, 2), amount.getEntries());
				var listItem = payload(Amount.class, value.getAmounts().get(0));
				assertEquals(1, value.getAmounts().size());
				assertEquals(31, listItem.getNumber());
				assertIterableEquals(List.of(3), listItem.getEntries());
				assertEquals(41, payload(Quantity.class, value.getQuantities().get(1)).getNumber());
				assertEquals(51, payload(Amount.class, value.getSortedAmounts().get(1)).getNumber());
			}

			close(nodes, leader);
			var newLeader = requireNonNull(nodes[awaitLeader(nodes)]);
			long cleared = write(newLeader, value -> {
				value.getAmount().clear();
				var removed = value.getAmounts().remove(0);
				payload(Amount.class, removed).getEntries().add(99);
				payload(Quantity.class, value.getQuantities().get(1)).setNumber(42);
				payload(Amount.class, value.getSortedAmounts().get(1)).setNumber(52);
			});
			var newSequence = requireNonNull(newLeader.getRaft()).getLogSequence();
			newSequence.snapshot();
			assertEquals(cleared, newSequence.getFirstIndex());
			long suffix = write(newLeader, value ->
					payload(Quantity.class, value.getQuantities().get(1)).setNumber(43));
			start(ports, nodes, leader);
			for (var node : nodes) {
				var online = requireNonNull(node);
				awaitApplied(online, suffix);
				var value = read(online);
				assertTrue(value.getAmount().isEmpty());
				assertTrue(value.getAmounts().isEmpty());
				assertEquals(43, payload(Quantity.class, value.getQuantities().get(1)).getNumber());
				assertEquals(52, payload(Amount.class, value.getSortedAmounts().get(1)).getNumber());
				assertEquals(List.of(2), List.copyOf(value.getNested().getSortedNumbers().keys()));
				assertIterableEquals(List.of(100L), payload(CustomPayload.class, value.getCustom()).getEntries());
			}
			assertEquals(cleared, requireNonNull(requireNonNull(nodes[leader]).getRaft()).getLogSequence().getFirstIndex());
		} finally {
			for (var node : nodes)
				if (node != null)
					requireNonNull(node.getRaft()).getServer().stop();
			for (int i = 0; i < nodes.length; i++)
				close(nodes, i);
		}
	}

	private void start(int[] ports, @Nullable Rocks[] nodes, int index) throws Exception {
		var xml = new StringBuilder("<raft Name=\"rocks-dynamic\">");
		for (int port : ports)
			xml.append("<node Host=\"127.0.0.1\" Port=\"").append(port).append("\"/>");
		var config = RaftConfig.loadFromString(xml.append("</raft>").toString());
		config.setDbHome(directory.resolve("node-" + index).toString());
		var rocks = new Rocks("127.0.0.1:" + ports[index], RocksMode.Pessimism, config, new Config(), false);
		try {
			AbstractDynamicFixture.RegisterRocksTables(rocks);
			table(rocks);
			requireNonNull(rocks.getRaft()).getServer().start();
			nodes[index] = rocks;
		} catch (Exception e) {
			try {
				rocks.close();
			} catch (Exception closeError) {
				e.addSuppressed(closeError);
			}
			throw e;
		}
	}

	private static <T extends Bean> T payload(Class<T> type, @Nullable DynamicBean slot) {
		assertNotNull(slot);
		var bean = slot.getBean();
		assertNotNull(bean);
		return assertInstanceOf(type, bean);
	}

	private static void close(@Nullable Rocks[] nodes, int index) {
		var rocks = nodes[index];
		nodes[index] = null;
		if (rocks != null)
			rocks.close();
	}

	private static long write(Rocks rocks, Consumer<Value> action) throws Exception {
		assertEquals(0, rocks.newProcedure(() -> {
			action.accept(table(rocks).getOrAdd(1));
			return 0;
		}).call());
		return requireNonNull(rocks.readAppliedWatermark()).index();
	}

	private static Value read(Rocks rocks) throws Exception {
		var values = new ArrayList<Value>(1);
		table(rocks).walk((key, value) -> values.add(value));
		assertEquals(1, values.size());
		return values.getFirst();
	}

	private static Table<Integer, Value> table(Rocks rocks) {
		return requireNonNull(rocks.<Integer, Value>getTableTemplate("DynamicValues")).openTable();
	}

	private static int awaitLeader(@Nullable Rocks[] nodes) throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
		do {
			for (int i = 0; i < nodes.length; i++) {
				var node = nodes[i];
				if (node != null && requireNonNull(node.getRaft()).isReadyLeader())
					return i;
			}
			//noinspection BusyWait
			Thread.sleep(25);
		} while (System.nanoTime() < deadline);
		return fail("Raft leader did not become ready");
	}

	private static void awaitApplied(Rocks rocks, long index) throws InterruptedException {
		var raft = requireNonNull(rocks.getRaft());
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
		do {
			raft.lock();
			try {
				if (raft.getLogSequence().getLastApplied() >= index)
					return;
			} finally {
				raft.unlock();
			}
			//noinspection BusyWait
			Thread.sleep(25);
		} while (System.nanoTime() < deadline);
		fail("Raft did not apply entry " + index);
	}

	private static int[] freePorts() throws Exception {
		try (var first = new ServerSocket(); var second = new ServerSocket(); var third = new ServerSocket()) {
			for (var socket : List.of(first, second, third))
				socket.bind(new InetSocketAddress("127.0.0.1", 0));
			return new int[]{first.getLocalPort(), second.getLocalPort(), third.getLocalPort()};
		}
	}
}
