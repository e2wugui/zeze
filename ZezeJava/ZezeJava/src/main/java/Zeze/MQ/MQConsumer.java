package Zeze.MQ;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import Zeze.Builtin.MQ.BOptions;
import Zeze.Builtin.MQ.Master.BMQInfo;
import Zeze.Net.Connector;
import org.jetbrains.annotations.NotNull;

/**
 * MQ 消费者：订阅 topic 的全部 Manager 分区，接收推送消息并转交 listener 回调。
 */
public class MQConsumer {
	private final MQListener listener;
	private final long sessionId;
	private final BMQInfo.Data info;
	// 并发集：构造线程建立 + MQAgent 路由对账链差量增补（Manager 换址迁移跟随），
	// 与 reSubscribeRound/退订的遍历并发（弱一致迭代足够：Subscribe/Unsubscribe 幂等）。
	private final Set<Connector> managers = ConcurrentHashMap.newKeySet();
	// close幂等标志：close即终态（重复close空转；重用需重新构造实例）。
	private final AtomicBoolean closed = new AtomicBoolean();

	public static Collection<MQConsumer> getConsumers() {
		return MQ.mqAgent.getConsumers().values();
	}

	public MQConsumer(String topic, MQListener listener) {
		this.listener = listener;

		// 先取引用再触网络：构造失败必须成对释放（引用泄漏会使静态agent的归零停机
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
		} catch (Exception e) {
			// 对齐同族 MQ.createMQ/openMQ 的 catch(Exception)：本 try 块底层经 GetReadySocket/await
			// 超时或中断时，Task.forceThrow 会 sneaky 抛出受检类型（TimeoutException/
			// InterruptedException，未经包装），catch(RuntimeException) 挡不住——clientReleaseRefs
			// 不可达=静态 agent 引用泄漏，归零停机永不触发。try 块无受检声明，精确 rethrow 保持原类型。
			MQ.clientReleaseRefs();
			throw e;
		}
	}

	public long getSessionId() {
		return sessionId;
	}

	// 该消费者订阅的全部manager连接：构造时按 Master 路由建立；MQAgent 路由对账在
	// Manager 换址迁移后差量补建新地址 connector（只增不减，见 MQAgent 路由对账）。
	// MQAgent重连重订阅由此派生connector→consumers映射，调用方不得修改。
	public @NotNull Set<Connector> getManagers() {
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
		// 幂等close：退订（必达移除consumers条目）+引用释放（归零触发静态agent停重连）。
		if (!closed.compareAndSet(false, true))
			return;
		try {
			MQ.mqAgent.unsubscribe(this, managers);
		} finally {
			MQ.clientReleaseRefs();
		}
	}
}
