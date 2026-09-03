package Zeze.Raft.RocksRaft;

import java.io.File;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Changes;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Changes.decode 对未知表模板名的处置（FND-R2-3 异常源）。
 * 滚动升级时，新版 leader 的 Changes.encode 携带 follower 未注册的表模板名，
 * follower 在 apply 路径 readLog 解码时经 Record.setTableByName 抛
 * IllegalStateException。修复前该异常无处置点：commitIndex 已推进、应答不发，
 * 重试时条目已存在且 leaderCommit==commitIndex，follower 转为正常 ack——
 * lastApplied 永久停滞但"健康"地计入多数派（其日志完整可当选 leader，
 * 当选后全集群写入卡死）。修复：LogSequence.readLogForApply /
 * followerOnAppendEntries 拷贝循环对 decode 失败 fatalKill（宁死不糊，
 * 与 StateMachine.logFactory 对未知日志类型的处置一致）。
 * fatalKill 会 halt 进程，无法在 JVM 内直接断言；这里单测确认异常源的
 * 行为契约：未知模板抛 IllegalStateException（由 LogSequence 的 fatalKill
 * 捕获），已注册模板正常解码。
 */
@Fast
public class TestChangesDecodeUnknownTemplate {
	private static final String raftName = "127.0.0.1:17640";
	private static final String dbHome = "TestChangesDecodeUnknownTemplate.raft";
	private static final String knownTemplate = "tKnownTemplate";

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17640" DbHome="TestChangesDecodeUnknownTemplate.raft">
					<node Host="127.0.0.1" Port="17640"/>
					<node Host="127.0.0.1" Port="17641"/>
					<node Host="127.0.0.1" Port="17642"/>
				</raft>
				""");
	}

	private static Rocks newRocks() throws Exception {
		var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false);
		rocks.registerTableTemplate(knownTemplate, Integer.class, Zeze.Builtin.TestRocks.BValue.class);
		return rocks;
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

	// 未知表模板（版本偏差：leader 注册了新表，本节点未注册）必须抛 IllegalStateException，
	// 这是 readLogForApply/fatalKillDecodeError 捕获并 fatalKill 的异常源。
	@Test
	public void testUnknownTemplateThrows() throws Exception {
		try (var rocks = newRocks()) {
			var changes = new Changes(rocks);
			// Changes.decode 的字段序：records.size, tableTemplateId, tableTemplateName, tableName, ...
			var bb = ByteBuffer.Allocate();
			bb.WriteUInt(1); // 1 record
			bb.WriteUInt(0); // tableTemplateId
			bb.WriteString("tVersionSkewNewTable"); // leader新版本的表模板名，本节点未注册
			bb.WriteString("t"); // tableName

			var ex = assertThrows(IllegalStateException.class,
					() -> changes.decode(ByteBuffer.Wrap(bb.CopyIf())));
			assertTrue(ex.getMessage().contains("unknown table template"), ex.getMessage());
			assertTrue(ex.getMessage().contains(knownTemplate), // 可用模板列表便于排查
					ex.getMessage());
		}
	}

	// 已注册模板名正常解码（正例对照，证明上面抛异常的原因是模板名而不是其他解码错误）。
	@Test
	public void testKnownTemplateDecodes() throws Exception {
		try (var rocks = newRocks()) {
			var changes = new Changes(rocks);
			var bb = ByteBuffer.Allocate();
			bb.WriteUInt(1); // 1 record
			bb.WriteUInt(0); // tableTemplateId
			bb.WriteString(knownTemplate);
			bb.WriteString("t"); // tableName
			bb.WriteInt(7); // key: int
			bb.WriteUInt(Changes.Record.Remove); // state: Remove（无后续字段）
			bb.WriteUInt(0); // atomicLongs.size

			changes.decode(ByteBuffer.Wrap(bb.CopyIf()));
			assertEquals(1, changes.getRecords().size());
			var record = changes.getRecords().values().iterator().next();
			assertEquals(Changes.Record.Remove, record.getState());
		}
	}
}
