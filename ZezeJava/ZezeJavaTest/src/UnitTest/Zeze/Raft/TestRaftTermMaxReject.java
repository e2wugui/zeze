package UnitTest.Zeze.Raft;

import java.io.File;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * FND6-08：恶意/损坏报文携带超大 term（如 Long.MAX_VALUE）会被 trySetTerm 无上界采纳并
 * 持久化，此后选举的 term+1 溢出回绕为负值永远无法推进（判 Older），选举永久冻结且重启
 * 不可恢复，超大 term 还会随投票传染其他节点。
 * 修复：trySetTerm 对超过 TERM_MAX 的 term 拒绝采纳（返回 Older 按陈旧处理，不持久化）；
 * 选举入口在 term 达到上界时拒绝发起（checkTermCanElect），杜绝 term+1 回绕。
 * 直接构造 Rocks/Raft（不 start server，无网络与选举流量），验证 term 采纳的纯语义。
 */
@Fast
public class TestRaftTermMaxReject {
	private static final String dbHome = "TestRaftTermMaxReject.raft";

	// 显式DbHome，避免RaftName改写默认DbHome；3节点仅是Raft构造的配置要求，
	// 本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17650" DbHome="TestRaftTermMaxReject.raft">
					<node Host="127.0.0.1" Port="17650"/>
					<node Host="127.0.0.1" Port="17651"/>
					<node Host="127.0.0.1" Port="17652"/>
				</raft>
				""");
	}

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
	}

	@AfterEach
	public void tearDown() {
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	@Test
	public void testOversizedTermRejected() throws Exception {
		var rocks = new Rocks("127.0.0.1:17650", RocksMode.Pessimism, newRaftConfig(), new Config(), false);
		try {
			var ls = rocks.getRaft().getLogSequence();

			// 正常采纳与比较语义不受影响。
			assertEquals(LogSequence.SetTermResult.Newer, ls.trySetTerm(5));
			assertEquals(LogSequence.SetTermResult.Same, ls.trySetTerm(5));
			assertEquals(LogSequence.SetTermResult.Older, ls.trySetTerm(3));

			// FND6-08 核心：恶意超大 term 被拒绝，不采纳、不持久化，term 保持不变。
			assertEquals(LogSequence.SetTermResult.Older, ls.trySetTerm(Long.MAX_VALUE));
			assertEquals(5, ls.getTerm(), "超大term不得被采纳");
			assertEquals(LogSequence.SetTermResult.Older, ls.trySetTerm(LogSequence.TERM_MAX + 1));
			assertEquals(5, ls.getTerm(), "恰超上界的term不得被采纳");

			// 攻击后选举仍可正常推进（无冻结）。
			assertEquals(LogSequence.SetTermResult.Newer, ls.trySetTerm(6));
			assertEquals(6, ls.getTerm());

			// 上界本身合法（= 不拒），之后 term+1 的回绕值被拒，且选举入口拒绝发起。
			assertEquals(LogSequence.SetTermResult.Newer, ls.trySetTerm(LogSequence.TERM_MAX));
			assertEquals(LogSequence.TERM_MAX, ls.getTerm());
			var checkTermCanElect = rocks.getRaft().getClass()
					.getDeclaredMethod("checkTermCanElect");
			checkTermCanElect.setAccessible(true);
			assertFalse((Boolean)checkTermCanElect.invoke(rocks.getRaft()),
					"term 达到上界后必须拒绝选举，防 term+1 溢出回绕");
		} finally {
			rocks.close();
		}
	}
}
