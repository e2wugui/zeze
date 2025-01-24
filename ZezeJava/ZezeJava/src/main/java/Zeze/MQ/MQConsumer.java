package Zeze.MQ;

import java.util.Collection;
import java.util.HashSet;
import Zeze.Builtin.MQ.BOptions;
import Zeze.Builtin.MQ.Master.BMQInfo;
import Zeze.Net.Connector;
import org.jetbrains.annotations.NotNull;

public class MQConsumer {
	private final MQListener listener;
	private final long sessionId;
	private final BMQInfo.Data info;
	private final HashSet<Connector> managers = new HashSet<>();

	public static Collection<MQConsumer> getConsumers() {
		return MQ.mqAgent.getConsumers().values();
	}

	public MQConsumer(String topic, MQListener listener) {
		this.listener = listener;

		MQ.masterAgent.startAndWaitConnectionReady();
		var servers = MQ.masterAgent.openMQ(topic);
		this.info = servers.getInfo();
		for (var server : servers.getServers()) {
			managers.add(MQ.mqAgent.getOrAddConnector(server.getHost(), server.getPort()));
		}
		this.sessionId = servers.getSessionId();
		MQ.mqAgent.subscribe(topic, sessionId, this, managers);
	}

	public long getSessionId() {
		return sessionId;
	}

	public String getTopic() {
		return info.getTopic();
	}

	public BOptions.Data getOptions() {
		return info.getOptions();
	}

	public int getPartition() {
		return info.getPartition();
	}

	public @NotNull MQListener getListener() {
		return listener;
	}

	public void close() {
		MQ.mqAgent.unsubscribe(this, managers);
	}
}
