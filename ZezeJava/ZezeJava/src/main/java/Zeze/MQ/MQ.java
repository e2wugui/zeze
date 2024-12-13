package Zeze.MQ;

import java.util.Arrays;
import java.util.Comparator;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BOptions;
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
	private static final MasterAgent masterAgent;
	private static final MQAgent mqAgent;

	static {
		masterAgent = new MasterAgent(Config.load());
		mqAgent = new MQAgent();
	}

	public static MQ createMQ(String topic, int partition, BOptions.Data options) {
		masterAgent.startAndWaitConnectionReady();
		return new MQ(masterAgent.openMQ(topic, partition, options));
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
		Arrays.sort(mqConnectors, new Comparator<MQConnector>() {
			@Override
			public int compare(MQConnector o1, MQConnector o2) {
				return Integer.compare(o1.server.getPartitionIndex(), o2.server.getPartitionIndex());
			}
		});
	}

	public void sendMessage(int hash, BMessage.Data message) {
		var index = hash % mqConnectors.length;
		var conn = mqConnectors[index];
		if (conn.server.getPartitionIndex() != index)
			throw new RuntimeException("fatal error, index mismatch: " + conn.server.getPartitionIndex() + "," + index);
		message.setPartitionIndex(index);
		MQAgent.sendMessageTo(message, conn.connector);
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
