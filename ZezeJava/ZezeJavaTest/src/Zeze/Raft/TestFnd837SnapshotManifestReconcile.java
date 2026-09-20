package Zeze.Raft;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND8-37→RFD1-03：legacy迁移引导测试——代际化后对账退役为一次性迁移。
 * 钉住三态与崩溃重入：S>F先推指针再改名；S==F（含推进后改名前崩溃重入）直接改名；
 * S<F防御删除；无manifest按N=F。构造Raft不起server，伪造磁盘形态后重启。
 */
@Fast
public class TestFnd837SnapshotManifestReconcile {
	private static final String raftName = "127.0.0.1:26370";
	private static final String dbHome = "a3_TestFnd837SnapshotManifest.raft";

	private Raft raft;

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:26370" DbHome="a3_TestFnd837SnapshotManifest.raft">
					<node Host="127.0.0.1" Port="26370"/>
					<node Host="127.0.0.1" Port="26371"/>
					<node Host="127.0.0.1" Port="26372"/>
				</raft>
				""");
	}

	private static Zeze.Raft.StateMachine newSm() {
		return new Zeze.Raft.StateMachine() {
			@Override
			public SnapshotResult snapshot(String path) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void loadSnapshot(String path) {
			}
		};
	}

	private Raft newRaft() throws Exception {
		raft = new Raft(newSm(), raftName, newRaftConfig());
		raft.getLogSequence().setWriteOptions(RocksDatabase.getDefaultWriteOptions());
		return raft;
	}

	private void closeRaft() {
		if (raft != null) {
			try {
				raft.getLogSequence().close();
				raft.shutdown();
			} catch (Exception ignore) {
			}
			raft = null;
		}
	}

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
	}

	@AfterEach
	public void tearDown() {
		closeRaft();
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	// 伪造legacy磁盘形态：logs=[0,5..12]、rafts.firstIndex=F、snapshot.dat带manifest S。
	private void forgeLegacyState(long firstIndex, Long manifest) throws Exception {
		var r = newRaft();
		var ls = r.getLogSequence();
		for (long i = 5; i <= 12; i++)
			ls.saveLog(new RaftLog(1, i, new HeartbeatLog()));
		saveFirstIndex(ls, firstIndex);
		var snap = newZipWithManifest(manifest);
		Files.move(snap, Paths.get(ls.getSnapshotFullName()), StandardCopyOption.REPLACE_EXISTING);
		closeRaft();
	}

	// legacy manifest读取往返：无entry→null；有entry可读回；文件不存在→null。
	@Test
	public void testManifestRead() throws Exception {
		var zip = newZipWithManifest(null);
		try {
			assertNull(LogSequence.readSnapshotManifest(zip), "无manifest entry返回null");
		} finally {
			Files.deleteIfExists(zip);
		}
		zip = newZipWithManifest(42L);
		try {
			assertEquals(42L, LogSequence.readSnapshotManifest(zip));
		} finally {
			Files.deleteIfExists(zip);
		}
		assertNull(LogSequence.readSnapshotManifest(Paths.get(dbHome, "a3_not_exist.zip")));
	}

	// 迁移窗口（S>F，迁移前崩溃留下的"内容超前索引"）：推进firstIndex到S并改名gen(S)。
	// 修复前（代际化前）：lastApplied=commitIndex=5，(5..10]已被快照包含却会重放（双重应用）。
	@Test
	public void testLegacyMigrateAdvances() throws Exception {
		forgeLegacyState(5, 10L);
		var r = newRaft();
		var ls = r.getLogSequence();
		assertEquals(10, ls.getFirstIndex(), "迁移必须推进firstIndex到manifest代次");
		assertEquals(10, ls.getLastApplied(), "lastApplied跳过已应用段");
		assertEquals(10, ls.getCommitIndex(), "commitIndex对齐");
		assertEquals(12, ls.getLastIndex(), "lastIndex不受影响");
		assertTrue(Files.exists(ls.genSnapshotPath(10)), "迁移后快照必须改名为gen(S)");
		assertFalse(Files.exists(Paths.get(ls.getSnapshotFullName())), "snapshot.dat必须消失");
	}

	// 崩溃重入（"推进后、改名前"崩溃）：指针已S、snapshot.dat仍在——重启触发条件成立，
	// S==F分支重跑改名收敛，唯一快照不丢（Rev4修正的反序缺陷正是这里）。
	@Test
	public void testLegacyMigrateReentryAfterAdvanceCrash() throws Exception {
		forgeLegacyState(10, 10L);
		var r = newRaft();
		var ls = r.getLogSequence();
		assertEquals(10, ls.getFirstIndex());
		assertTrue(Files.exists(ls.genSnapshotPath(10)), "重入必须完成改名，快照不丢");
		assertFalse(Files.exists(Paths.get(ls.getSnapshotFullName())));
	}

	// 旧格式快照（无manifest）：按N=F改名，不对账。
	@Test
	public void testLegacyMigrateOldFormat() throws Exception {
		forgeLegacyState(5, null);
		var r = newRaft();
		var ls = r.getLogSequence();
		assertEquals(5, ls.getFirstIndex(), "无代次信息不得对账");
		assertEquals(5, ls.getLastApplied());
		assertTrue(Files.exists(ls.genSnapshotPath(5)), "按N=F改名为gen(F)");
	}

	// S<F（正常流程不可达的防御方向）：丢弃快照，firstIndex保持，不产生gen。
	@Test
	public void testLegacyMigrateBehindDiscardsSnapshot() throws Exception {
		forgeLegacyState(5, 3L);
		var r = newRaft();
		var ls = r.getLogSequence();
		assertEquals(5, ls.getFirstIndex(), "S<F不推进firstIndex");
		assertFalse(Files.exists(ls.genSnapshotPath(3)), "过期快照必须丢弃");
		assertFalse(Files.exists(Paths.get(ls.getSnapshotFullName())), "snapshot.dat必须删除");
		assertTrue(Files.exists(Paths.get(dbHome)), "dbHome仍在");
	}

	// saveFirstIndex 是 private，按 TestFnd757 的先例反射调用。
	private static void saveFirstIndex(LogSequence ls, long value) throws Exception {
		Method method = LogSequence.class.getDeclaredMethod("saveFirstIndex", long.class);
		method.setAccessible(true);
		try {
			method.invoke(ls, value);
		} catch (InvocationTargetException e) {
			throw Task.forceThrow(e.getCause());
		}
	}

	// 构造测试用快照zip（manifest为null时不含manifest entry）。
	private static Path newZipWithManifest(Long manifest) throws Exception {
		var zipPath = Files.createTempFile("a3_fnd837_snap", ".zip");
		Files.delete(zipPath); // 空文件不是合法zip，删除后由ZipFileSystem新建
		try (var zipFs = FileSystems.newFileSystem(zipPath, Map.of("create", "true"))) {
			Files.writeString(zipFs.getPath("placeholder"), "fnd837");
			if (manifest != null)
				Files.writeString(zipFs.getPath(LogSequence.snapshotManifestEntryName), Long.toString(manifest));
		}
		return zipPath;
	}
}
