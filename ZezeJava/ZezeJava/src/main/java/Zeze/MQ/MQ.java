package Zeze.MQ;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BOptions;
import Zeze.Builtin.MQ.BSendMessage;
import Zeze.Builtin.MQ.Master.BMQInfo;
import Zeze.Builtin.MQ.Master.BMQServer;
import Zeze.Builtin.MQ.Master.BMQServers;
import Zeze.Config;
import Zeze.MQ.Master.MasterAgent;
import Zeze.Net.Connector;

/**
 * 一个topic队列实现，包含多个分区partition.
 *
 * 生产者通过这里发送消息。
 * 消费者也由这里驱动(todo)。
 *
 * masterAgent,mqAgent都是静态的(static)，整个进程共享。
 * 生命周期（GB-D04）：静态agent按引用计数归零停connector重连；本类close()幂等且close即终态
 * （重用需createMQ/openMQ重建实例）；进程退出用 {@link #shutdown()} 强制全停（此后客户端入口明确报错）。
 */
public class MQ {
	static final MasterAgent masterAgent;
	static final MQAgent mqAgent;

	static {
		masterAgent = new MasterAgent(Config.load());
		mqAgent = new MQAgent();
	}

	public static MQ createMQ(String topic, int partition, BOptions.Data options) throws Exception {
		// BOptions 校验（fail-fast）：DoubleWrite/Raft3 尚未实现（见 advanced-mq.md），明确报错拒绝，
		// 不再静默按 Single 跑——否则协议层回显成功掩盖可靠性降级（Manager 磁盘损坏即数据全失）。
		// 0/不传=默认 Single，兼容既有 null 调用形态。Master 端有同款校验兜底（直连 MasterAgent 的调用方）。
		var optionsValue = null != options ? options.getOptions() : BOptions.Single;
		if (optionsValue != BOptions.Single && optionsValue != 0)
			throw new IllegalArgumentException("createMQ options=" + optionsValue + " 未实现：当前仅实现 Single("
					+ BOptions.Single + ")，DoubleWrite(" + BOptions.DoubleWrite + ")/Raft3(" + BOptions.Raft3
					+ ") 拒绝创建，不再静默降级");
		clientAddRefs();
		try {
			masterAgent.startAndWaitConnectionReady();
			mqAgent.start();
			return new MQ(masterAgent.createMQ(topic, partition, options));
		} catch (Exception e) {
			// 【GB-D04】构造失败必须成对释放（引用泄漏会使静态agent的归零停机永不触发）。
			clientReleaseRefs();
			throw e;
		}
	}

	public static MQ openMQ(String topic) throws Exception {
		clientAddRefs();
		try {
			masterAgent.startAndWaitConnectionReady();
			mqAgent.start();
			return new MQ(masterAgent.openMQ(topic));
		} catch (Exception e) {
			clientReleaseRefs();
			throw e;
		}
	}

	/*
	public static MQ alterMQ(String topic, int partition) {
		masterAgent.startAndWaitConnectionReady();
		return new MQ(masterAgent.openMQ(topic, partition, null));
	}
	*/

	// 【GB-D04】成对获取两个静态agent的引用：任一失败（MQ.shutdown()后addRef明确报错）回滚已加部分。
	// 包内可见：MQConsumer构造同用。
	static void clientAddRefs() {
		masterAgent.addRef();
		try {
			mqAgent.addRef();
		} catch (RuntimeException e) {
			masterAgent.release();
			throw e;
		}
	}

	// 【GB-D04】成对释放；归零触发agent停connector重连（见MQAgent/MasterAgent.release）。
	static void clientReleaseRefs() {
		mqAgent.release();
		masterAgent.release();
	}

	/**
	 * 【GB-D04】显式全局停机（进程退出钩子形态，拍板双轨之一）：强制停两个静态agent
	 * （不等引用归零）——停connector重连、关socket。幂等；此后 createMQ/openMQ/MQConsumer
	 * 构造在 agent.addRef 处明确报错（终态，重用需新进程）。
	 */
	public static void shutdown() {
		mqAgent.shutdown();
		masterAgent.shutdown();
	}

	private final MQConnector[] mqConnectors;
	private final BMQInfo.Data info;
	// 【GB-D04】close幂等标志：close即终态，不做复活（对齐仓内Application一次性实例口径）。
	private final AtomicBoolean closed = new AtomicBoolean();

	protected MQ(BMQServers.Data servers) {
		this.info = servers.getInfo();
		mqConnectors = new MQConnector[servers.getServers().size()];
		int i = 0;
		for (var server : servers.getServers()) {
			var connector = mqAgent.getOrAddConnector(server.getHost(), server.getPort());
			mqConnectors[i++] = new MQConnector(server, connector);
		}
		Arrays.sort(mqConnectors, Comparator.comparingInt(o -> o.server.getPartitionIndex()));
	}

	public void sendMessage(int hash, BMessage.Data message) {
		// 【GB-D04】close后使用明确报错（不是无声空转）。
		if (closed.get())
			throw new IllegalStateException("MQ closed; re-create via createMQ/openMQ to send again. topic="
					+ info.getTopic());
		// 查找发送队列服务器
		var index = Integer.remainderUnsigned(hash, mqConnectors.length);
		//System.out.println("hash = " + hash + " " + index);
		var conn = mqConnectors[index];
		if (conn.server.getPartitionIndex() != index)
			throw new RuntimeException("fatal error, index mismatch: " + conn.server.getPartitionIndex() + "," + index);

		message.setTimestamp(System.currentTimeMillis());
		var sendMessage = new BSendMessage.Data(conn.server.getTopic(), index, message);
		MQAgent.sendMessageTo(sendMessage, conn.connector);
	}

	public BMQInfo.Data getInfo() {
		return info;
	}

	public void close() {
		// 【GB-D04】幂等close+引用释放：引用归零触发静态agent停connector重连。
		if (!closed.compareAndSet(false, true))
			return;
		clientReleaseRefs();
	}

	static class MQConnector {
		private final BMQServer.Data server;
		private final Connector connector;

		public MQConnector(BMQServer.Data server, Connector connector) {
			this.server = server;
			this.connector = connector;
		}
	}
}
