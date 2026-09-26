package Zeze.MQ;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import Zeze.Builtin.MQ.BOptions;
import Zeze.Builtin.MQ.Master.BMQInfo;
import Zeze.Net.Connector;
import org.jetbrains.annotations.NotNull;

public class MQConsumer {
	private final MQListener listener;
	private final long sessionId;
	private final BMQInfo.Data info;
	private final HashSet<Connector> managers = new HashSet<>();
	// 【GB-D04】close幂等标志：close即终态（重复close空转；重用需重新构造实例）。
	private final AtomicBoolean closed = new AtomicBoolean();

	public static Collection<MQConsumer> getConsumers() {
		return MQ.mqAgent.getConsumers().values();
	}

	public MQConsumer(String topic, MQListener listener) {
		this.listener = listener;

		// 【GB-D04】先取引用再触网络：构造失败必须成对释放（引用泄漏会使静态agent的归零停机
		// 永不触发——connector无限重连正是要消灭的残留形态）；MQ.shutdown()后addRef在此明确报错。
		MQ.clientAddRefs();
		try {
			MQ.masterAgent.startAndWaitConnectionReady();
			var servers = MQ.masterAgent.openMQ(topic);
			this.info = servers.getInfo();
			for (var server : servers.getServers()) {
				managers.add(MQ.mqAgent.getOrAddConnector(server.getHost(), server.getPort()));
			}
			this.sessionId = servers.getSessionId();
			MQ.mqAgent.subscribe(topic, sessionId, this, managers);
		} catch (RuntimeException e) {
			MQ.clientReleaseRefs();
			throw e;
		}
	}

	public long getSessionId() {
		return sessionId;
	}

	// 只读约定：该消费者订阅的全部manager连接（构造时确定，不再变更）。
	// MQAgent重连重订阅由此派生connector→consumers映射，调用方不得修改。
	public @NotNull HashSet<Connector> getManagers() {
		return managers;
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
		// 【GB-D04】幂等close：退订（必达移除consumers条目）+引用释放（归零触发静态agent停重连）。
		if (!closed.compareAndSet(false, true))
			return;
		try {
			MQ.mqAgent.unsubscribe(this, managers);
		} finally {
			MQ.clientReleaseRefs();
		}
	}
}
