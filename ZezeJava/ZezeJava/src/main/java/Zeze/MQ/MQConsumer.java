package Zeze.MQ;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.MQ.BOptions;
import Zeze.Builtin.MQ.Master.BMQInfo;
import Zeze.Builtin.MQ.Subscribe;
import Zeze.IModule;
import Zeze.Net.Connector;
import org.jetbrains.annotations.NotNull;

public class MQConsumer {
	private final MQListener listener;
	private final long sessionId;
	private final BMQInfo.Data info;

	private final static AtomicLong sessionIdGen = new AtomicLong();

	public static Collection<MQConsumer> getConsumers() {
		return MQ.mqAgent.getConsumers().values();
	}

	public MQConsumer(String topic, MQListener listener) {
		this.listener = listener;

		MQ.masterAgent.startAndWaitConnectionReady();
		var servers = MQ.masterAgent.openMQ(topic);
		this.info = servers.getInfo();
		var managers = new HashSet<Connector>();
		for (var server : servers.getServers()) {
			managers.add(MQ.mqAgent.getOrAddConnector(server.getHost(), server.getPort()));
		}
		this.sessionId = sessionIdGen.incrementAndGet();

		// subscribe all
		var futures = new ArrayList<Subscribe>();
		for (var manager : managers) {
			var future = MQ.mqAgent.subscribe(topic, sessionId, this, manager);
			if (null != future)
				futures.add(future);
		}

		// await all
		for (var future : futures) {
			assert future.getFuture() != null;
			future.getFuture().await();
		}

		// check all result code
		for (var future : futures) {
			if (future.getResultCode() != 0)
				throw new RuntimeException("subscribe consumer error=" + IModule.getErrorCode(future.getResultCode()));
		}
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
		MQ.mqAgent.unsubscribe(this);
	}
}
