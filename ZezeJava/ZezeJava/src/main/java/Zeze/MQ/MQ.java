package Zeze.MQ;

import java.util.Arrays;
import java.util.Comparator;
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
		masterAgent.startAndWaitConnectionReady();
		mqAgent.start();
		return new MQ(masterAgent.createMQ(topic, partition, options));
	}

	public static MQ openMQ(String topic) throws Exception {
		masterAgent.startAndWaitConnectionReady();
		mqAgent.start();
		return new MQ(masterAgent.openMQ(topic));
	}

	/*
	public static MQ alterMQ(String topic, int partition) {
		masterAgent.startAndWaitConnectionReady();
		return new MQ(masterAgent.openMQ(topic, partition, null));
	}
	*/

	private final MQConnector[] mqConnectors;
	private final BMQInfo.Data info;

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
		// reserve
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
