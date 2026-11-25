package Zeze.Dbh2;

import static java.util.Objects.requireNonNull;

import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.SplitPut;
import Zeze.Net.Binary;
import Zeze.Raft.Agent;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Dbh2Agent 带 NetClientFactory 的构造器（分桶同步客户端 dbh2Splitting 使用）必须与常规构造器一致，
 * 将收发缓冲上限放宽到 100MB，否则 >2MB 的合法 SplitPut 批次会被 SocketOptions 默认 2MB 上限静默丢弃。
 */
@Fast
public class TestDbh2AgentFactoryBufferLimits {
	private static final int EXPECTED_MAX_BUFFER = 100 * 1024 * 1024;

	private static int[] allocatePorts(int count) throws Exception {
		var sockets = new ArrayList<ServerSocket>(count);
		try {
			var ports = new int[count];
			for (int i = 0; i < count; ++i) {
				var ss = new ServerSocket(0);
				sockets.add(ss);
				ports[i] = ss.getLocalPort();
			}
			return ports;
		} finally {
			for (var ss : sockets)
				ss.close();
		}
	}

	private static String buildRaftConfig(int[] ports) {
		var sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<raft Name=\"\">\n");
		for (int port : ports)
			sb.append("    <node Host=\"127.0.0.1\" Port=\"").append(port).append("\"/>\n");
		sb.append("</raft>\n");
		return sb.toString();
	}

	@Test
	void testNetClientFactoryConstructorSets100MbSocketBufferLimits() throws Exception {
		Task.tryInitThreadPool();
		var raftConfig = buildRaftConfig(allocatePorts(1));
		var agent = new Dbh2Agent(raftConfig, (a, name, cfg) -> new Agent.NetClient(a, name, cfg));
		try {
			var socketOptions = requireNonNull(agent.getRaftAgent().getClient()).getConfig().getSocketOptions();
			Assertions.assertEquals(EXPECTED_MAX_BUFFER, socketOptions.getInputBufferMaxProtocolSize(),
					"带 NetClientFactory 构造器必须设置 100MB InputBufferMaxProtocolSize");
			Assertions.assertEquals(EXPECTED_MAX_BUFFER, socketOptions.getOutputBufferMaxSize(),
					"带 NetClientFactory 构造器必须设置 100MB OutputBufferMaxSize");
		} finally {
			agent.close();
		}
	}

	@Test
	void testNetClientFactoryAgentSendsSplitPutExceeding2Mb(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var raftConfig = buildRaftConfig(allocatePorts(3));
		var database = new RocksDatabase(tempDir.resolve("large_split_put").toString());
		var taskOneByOne = new TaskOneByOneByKey();
		var nodes = RaftBucketTopologySupport.startBucket(database, raftConfig, tempDir, taskOneByOne);
		var splitAgent = new Dbh2Agent(raftConfig, (a, name, cfg) -> new Agent.NetClient(a, name, cfg));
		try {
			RaftBucketTopologySupport.waitLeader(nodes);
			var meta = new BBucketMeta.Data();
			meta.setDatabaseName("database");
			meta.setTableName("table1");
			meta.setRaftConfig("");
			meta.setKeyFirst(Binary.Empty);
			meta.setKeyLast(Binary.Empty);
			splitAgent.setBucketMeta(meta);

			// 构造单批总编码体积 > 2.5MB 的 SplitPut（100 条 × 26KB，超过默认 2MB 缓冲上限）。
			var splitPut = new SplitPut();
			splitPut.Argument.setFromTransaction(false);
			var payload = new byte[26 * 1024];
			Arrays.fill(payload, (byte)0x5A);
			var value = new Binary(payload);
			for (int i = 0; i < 100; ++i) {
				var keyBytes = new byte[]{0, (byte)(i >>> 8), (byte)i};
				splitPut.Argument.getPuts().put(new Binary(keyBytes), value);
			}
			splitPut.setTimeout(15_000);

			var future = splitAgent.getRaftAgent().sendForWait(splitPut);
			Assertions.assertTrue(future.await(20_000L),
					"超过 2MB 的 SplitPut 必须在超时前成功送达并应答");
			Assertions.assertEquals(0, splitPut.getResultCode(),
					"超过 2MB 的 SplitPut 必须成功落盘");

			var probeKey = new Binary(new byte[]{0, 0, 99});
			var got = RaftBucketTopologySupport.get(splitAgent, probeKey);
			Assertions.assertEquals(value, got, "大批量 SplitPut 写入的记录必须可读回");
		} finally {
			RaftBucketTopologySupport.stopBucket(nodes, splitAgent, database);
		}
	}
}
