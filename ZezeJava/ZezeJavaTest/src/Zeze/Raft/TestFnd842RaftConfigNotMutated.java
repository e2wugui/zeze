package Zeze.Raft;

import java.io.File;

import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * FND8-42回归：Raft构造器对非空RaftName直接变异调用方传入的RaftConfig
 * （setName/setDbHome）——共享同一配置实例的多Raft场景，第二个Raft构造后
 * 第一个Raft的getName()/getDbHome()全部指向第二个的值，快照路径
 * （getSnapshotFullName）错位造成跨实例数据覆盖。仓内调用点均传独立实例未触发，
 * 属潜伏路径。
 * 修复：Raft构造器经raftConf.derive(RaftName)在私有副本上改名（保留
 * "DbHome未特别配置时跟随RaftName"的联动语义），永不变异调用方对象；
 * 独立配置调用点可见行为不变。
 * 纯单元：两个Raft共用同一RaftConfig（不起server，无端口占用；xml不配DbHome
 * →dbHome与Name相关→各自派生独立目录，无RocksDB锁冲突）。
 */
@Fast
public class TestFnd842RaftConfigNotMutated {
	private static final String nameA = "127.0.0.1:26380";
	private static final String nameB = "127.0.0.1:26381";
	private static final String dirA = nameA.replace(':', '_');
	private static final String dirB = nameB.replace(':', '_');

	private Raft raftA;
	private Raft raftB;

	// 不配DbHome：dbHome与Name相关，改名时联动派生（原语义）。
	private static RaftConfig newSharedConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:26380">
					<node Host="127.0.0.1" Port="26380"/>
					<node Host="127.0.0.1" Port="26381"/>
					<node Host="127.0.0.1" Port="26382"/>
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

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
	}

	@AfterEach
	public void tearDown() throws Exception {
		for (var raft : new Raft[]{raftA, raftB}) {
			if (raft != null) {
				try {
					raft.getLogSequence().close();
					raft.shutdown();
				} catch (Exception ignore) {
				}
			}
		}
		LogSequence.deleteDirectory(new File(dirA)); // best-effort
		LogSequence.deleteDirectory(new File(dirB));
	}

	// 同一RaftConfig构造两个Raft：调用方对象不被变异，两个Raft各持私有副本，
	// 互不污染（修复前第二个构造后第一个的name/dbHome跟着变）。
	@Test
	public void testSharedConfigNotMutated() throws Exception {
		var shared = newSharedConfig();
		var originalName = shared.getName();
		var originalDbHome = shared.getDbHome();

		raftA = new Raft(newSm(), nameA, shared);
		assertEquals(originalName, shared.getName(), "构造Raft不得变异传入配置的Name");
		assertEquals(originalDbHome, shared.getDbHome(), "构造Raft不得变异传入配置的DbHome");
		assertNotEquals(shared, raftA.getRaftConfig(), "Raft必须持有私有副本");
		assertEquals(dirA, raftA.getRaftConfig().getDbHome(), "副本DbHome按RaftName联动派生");

		raftB = new Raft(newSm(), nameB, shared);
		assertEquals(originalName, shared.getName(), "第二个Raft同样不得变异共享配置");
		assertEquals(originalDbHome, shared.getDbHome());

		// 关键断言：A不被B污染（修复前A.getRaftConfig()==shared，name/dbHome已变B的）。
		assertEquals(nameA.replace(':', '_'), raftA.getRaftConfig().getName(),
				"A的配置Name不得被B的构造污染");
		assertEquals(dirA, raftA.getRaftConfig().getDbHome(), "A的DbHome不得被B污染（快照路径错位）");
		assertEquals(nameB.replace(':', '_'), raftB.getRaftConfig().getName());
		assertEquals(dirB, raftB.getRaftConfig().getDbHome());
	}

	// 显式DbHome（与Name无关）不联动改写：派生副本保持显式值（原语义保留）。
	@Test
	public void testExplicitDbHomePreserved() {
		var conf = RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:26380" DbHome="a3_fnd842_explicit">
					<node Host="127.0.0.1" Port="26380"/>
					<node Host="127.0.0.1" Port="26381"/>
					<node Host="127.0.0.1" Port="26382"/>
				</raft>
				""");
		var derived = conf.derive(nameB);
		assertEquals(nameB.replace(':', '_'), derived.getName());
		assertEquals("a3_fnd842_explicit", derived.getDbHome(), "显式DbHome不随RaftName联动");
		assertEquals(conf.getName(), "127.0.0.1_26380", "derive不得变异原对象");
		assertEquals("a3_fnd842_explicit", conf.getDbHome());
		// 可变标量完整复制。
		assertEquals(conf.getAppendEntriesTimeout(), derived.getAppendEntriesTimeout());
		assertEquals(conf.getSnapshotLogCount(), derived.getSnapshotLogCount());
		assertEquals(conf.getNodes().size(), derived.getNodes().size());
	}
}
