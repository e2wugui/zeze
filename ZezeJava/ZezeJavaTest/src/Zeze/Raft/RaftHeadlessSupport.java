package Zeze.Raft;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import Zeze.Raft.RocksRaft.Changes;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Raft.RocksRaft.TestFlushRetryApply.BListBean;
import Zeze.Raft.RocksRaft.Transaction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.LongConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * headless Rocks Raft 测试支撑：不开server、无网络的构造与驱动工具，复用
 * TestFlushRetryApply的非幂等CollList1载体（LogList1&lt;Integer&gt;的解码工厂由各测试
 * setUp自注册——生成代码场景由生成的注册代码负责，自建表模板需自注册）。
 * 3节点仅是Raft构造的配置要求，不占用任何端口。
 */
final class RaftHeadlessSupport {
	private RaftHeadlessSupport() {
	}

	// raftName与节点端口由号段桌id派生（30000+id）：本族号段7750-7781对应端口37750-37782，
	// 已核无其他测试占用；3节点=port、port+1、port+2。@Fast类并行的唯一性走
	// FastServerIds桌，不再共用固定字面量。
	static String raftName(int serverId) {
		return "127.0.0.1:" + (30000 + serverId);
	}

	/** 显式DbHome的3节点配置（Name与端口由serverId派生，见{@link #raftName(int)}）。 */
	static RaftConfig newRaftConfig(int serverId, String dbHome) {
		var name = raftName(serverId);
		var port = 30000 + serverId;
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="%s" DbHome="%s">
					<node Host="127.0.0.1" Port="%d"/>
					<node Host="127.0.0.1" Port="%d"/>
					<node Host="127.0.0.1" Port="%d"/>
				</raft>
				""".formatted(name, dbHome, port, port + 1, port + 2));
	}

	/**
	 * 收集普通（非unique）增量Changes：list追加item的OP_ADD（重放不幂等，双重应用的载体）。
	 * encode内部对每个Record调setTableByName解析table（对齐leader的saveLog前置步骤）。
	 */
	static Changes captureChanges(Rocks rocks, Table<Integer, BListBean> table, int item) throws Exception {
		final Transaction[] ts = new Transaction[1];
		var rc = rocks.newProcedure(() -> {
			ts[0] = Transaction.getCurrent();
			table.getOrAdd(1).getList().add(item);
			return 0L;
		}).call();
		assertEquals(Zeze.Transaction.Procedure.RaftRetry, rc); // not leader
		var changes = ts[0].getChanges();
		assertNotNull(changes);
		changes.encode(ByteBuffer.Allocate());
		return changes;
	}

	/** 在captureChanges基础上补unique请求字段（requestId=1）与rpcResult——水位终态存根的载体。 */
	static Changes captureUniqueChanges(Rocks rocks, Table<Integer, BListBean> table,
										String clientId, Zeze.Net.Binary rpcResult) throws Exception {
		var changes = captureChanges(rocks, table, 30);
		changes.getUnique().setRequestId(1);
		changes.getUnique().setClientId(clientId);
		changes.setRpcResult(rpcResult);
		changes.setCreateTime(System.currentTimeMillis());
		return changes;
	}

	/** 模拟换主后同请求重发到达本节点的协议对象（终态存根/pre-apply存根的查询载体）。 */
	static Zeze.Builtin.ServiceManagerWithRaft.Login newRetriedRpc(Changes changes, String clientId) {
		var retried = new Zeze.Builtin.ServiceManagerWithRaft.Login();
		retried.getUnique().setRequestId(1);
		retried.getUnique().setClientId(clientId);
		retried.setCreateTime(changes.getCreateTime());
		return retried;
	}

	/** leader式应用一条非unique日志（term=1，原始对象进leaderAppendLogs走leaderApply）。 */
	static void applyEntry(Rocks rocks, Table<Integer, BListBean> table, long index, int item) throws Exception {
		var logSequence = rocks.getRaft().getLogSequence();
		var raftLog = new RaftLog(1, index, captureChanges(rocks, table, item));
		logSequence.saveLog(raftLog);
		leaderAppendLogsOf(logSequence).put(index, raftLog);
		logSequence.tryApply(raftLog, 1);
	}

	/** 直写存储层做初始数据（绕过事务，模拟前序条目已成功应用落盘的状态）。 */
	static void seedStorage(Table<Integer, BListBean> table, int key, Integer... items) throws Exception {
		var seed = table.newValue();
		for (var item : items)
			seed.getList().add(item); // 未托管，直接修改
		var keyBB = ByteBuffer.Allocate();
		table.encodeKey(keyBB, key);
		var valBB = ByteBuffer.Allocate();
		seed.encode(valBB);
		table.getRocksTable().put(keyBB.CopyIf(), valBB.CopyIf());
	}

	/** 读存储层的最终值（不经缓存，验证flush真的落盘）。 */
	static List<Integer> readStorage(Table<Integer, BListBean> table, int key) throws Exception {
		var keyBB = ByteBuffer.Allocate();
		table.encodeKey(keyBB, key);
		var bytes = table.getRocksTable().get(keyBB.CopyIf());
		var out = new ArrayList<Integer>();
		if (bytes != null) {
			var value = table.newValue();
			value.decode(ByteBuffer.Wrap(bytes));
			for (var item : value.getList())
				out.add(item);
		}
		return out;
	}

	/** 反射取leaderAppendLogs（模拟appendLog对leader请求的登记：tryApply优先取原始对象）。 */
	@SuppressWarnings("unchecked")
	static LongConcurrentHashMap<RaftLog> leaderAppendLogsOf(LogSequence logSequence) throws Exception {
		var field = LogSequence.class.getDeclaredField("leaderAppendLogs");
		field.setAccessible(true);
		return (LongConcurrentHashMap<RaftLog>)field.get(logSequence);
	}
}
