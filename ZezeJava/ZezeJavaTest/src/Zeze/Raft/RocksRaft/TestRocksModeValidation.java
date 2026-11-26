package Zeze.Raft.RocksRaft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import Zeze.Config;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.Server;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
class TestRocksModeValidation {
	private static final String RAFT_NAME = "127.0.0.1:17911";

	@TempDir
	Path tempDir;

	private static RaftConfig newRaftConfig(Path dbHome) {
		var config = RaftConfig.loadFromString("""
				<raft Name="127.0.0.1:17911">
				    <node Host="127.0.0.1" Port="17911"/>
				    <node Host="127.0.0.1" Port="17912"/>
				    <node Host="127.0.0.1" Port="17913"/>
				</raft>
				""");
		config.setDbHome(dbHome.toString());
		return config;
	}

	@Test
	void optimisticEnumConstantIsDeprecated() throws NoSuchFieldException {
		assertTrue(RocksMode.class.getField("Optimistic").isAnnotationPresent(Deprecated.class));
		assertFalse(RocksMode.class.getField("Pessimism").isAnnotationPresent(Deprecated.class));
	}

	@Test
	@SuppressWarnings("deprecation")
	void fourArgConstructorRejectsOptimisticWithoutCreatingDb() {
		var dbHome = tempDir.resolve("optimistic-4arg");
		var ex = assertThrows(UnsupportedOperationException.class,
				() -> new Rocks(RAFT_NAME, RocksMode.Optimistic, newRaftConfig(dbHome), new Config()));
		assertTrue(ex.getMessage().contains("Optimistic"), () -> "unexpected message: " + ex.getMessage());
		assertTrue(ex.getMessage().contains("Pessimism"), () -> "unexpected message: " + ex.getMessage());
		assertFalse(Files.exists(dbHome), "fail-fast must not create DbHome or RocksDB files");
	}

	@Test
	@SuppressWarnings("deprecation")
	void sixArgConstructorRejectsOptimisticBeforeCreatingRaftOrDb() {
		var dbHome = tempDir.resolve("optimistic-6arg");
		var serverFactoryCalled = new AtomicBoolean();
		var ex = assertThrows(UnsupportedOperationException.class,
				() -> new Rocks(RAFT_NAME, RocksMode.Optimistic, newRaftConfig(dbHome), new Config(),
						(raft, name, cfg) -> {
							serverFactoryCalled.set(true);
							return new Server(raft, name, cfg);
						},
						new TaskOneByOneByKey(1)));
		assertTrue(ex.getMessage().contains("Optimistic"), () -> "unexpected message: " + ex.getMessage());
		assertFalse(serverFactoryCalled.get(), "fail-fast must reject before invoking serverFactory / constructing Raft");
		assertFalse(Files.exists(dbHome), "fail-fast must not create DbHome or RocksDB files");
	}

	@Test
	@SuppressWarnings("DataFlowIssue")
	void constructorsRejectNullModeWithoutCreatingRaftOrDb() {
		var dbHome4 = tempDir.resolve("null-4arg");
		var ex4 = assertThrows(NullPointerException.class,
				() -> new Rocks(RAFT_NAME, null, newRaftConfig(dbHome4), new Config()));
		assertTrue(ex4.getMessage().contains("Pessimism"), () -> "unexpected message: " + ex4.getMessage());
		assertFalse(Files.exists(dbHome4), "null mode fail-fast must not create DbHome");

		var dbHome6 = tempDir.resolve("null-6arg");
		var serverFactoryCalled = new AtomicBoolean();
		var ex6 = assertThrows(NullPointerException.class,
				() -> new Rocks(RAFT_NAME, null, newRaftConfig(dbHome6), new Config(),
						(raft, name, cfg) -> {
							serverFactoryCalled.set(true);
							return new Server(raft, name, cfg);
						},
						new TaskOneByOneByKey(1)));
		assertTrue(ex6.getMessage().contains("Pessimism"), () -> "unexpected message: " + ex6.getMessage());
		assertFalse(serverFactoryCalled.get(), "null mode fail-fast must reject before constructing Raft");
		assertFalse(Files.exists(dbHome6), "null mode fail-fast must not create DbHome");
	}

	@Test
	void pessimismModeConstructsAndRunsProcedureNormally() throws Exception {
		Task.tryInitThreadPool();
		var dbHome = tempDir.resolve("pessimism-ok");
		try (var rocks = new Rocks(RAFT_NAME, RocksMode.Pessimism, newRaftConfig(dbHome), new Config())) {
			assertEquals(RocksMode.Pessimism, rocks.getRocksMode());
			assertNotNull(rocks.getStorage());
			assertTrue(Files.isDirectory(dbHome.resolve("statemachine")));
			assertEquals(0L, rocks.newProcedure(() -> 0L).call());
		}
	}
}
