package Zeze.MQ.Master;

import Zeze.Builtin.MQ.Master.CreateMQ;
import Zeze.Builtin.MQ.Master.CreatePartition;
import Zeze.Builtin.MQ.Master.ReportLoad;
import Zeze.Builtin.MQ.Master.ReportPartitions;
import Zeze.Builtin.MQ.Master.BReportPartitions;
import Zeze.Builtin.MQ.Master.DeletePartition;
import Zeze.Builtin.MQ.BOptions;
import Zeze.Builtin.MQ.Master.BMQServers;
import Zeze.Builtin.MQ.Master.OpenMQ;
import Zeze.Builtin.MQ.Master.Subscribe;
import Zeze.Builtin.MQ.Master.Register;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.ProtocolHandle;
import Zeze.Transaction.Procedure;
import org.jetbrains.annotations.NotNull;

public class MasterAgent extends AbstractMasterAgent {
	public static final String eServiceName = "Zeze.MQ.Master.Agent";
	private final Service service;
	private final ProtocolHandle<CreatePartition> createPartitionHandle;
	// 【GB-D01】Master 对账裁决孤儿后下发的删除（仅 Manager 形态的 agent 持有；客户端形态为 null → NotImplement）
	private final ProtocolHandle<DeletePartition> deletePartitionHandle;

	// 【GB-D04】客户端生命周期（拍板方案A，与MQAgent同型）：静态共享的agent引用计数，归零时停
	// connector重连（不再退避续排）；新引用复活（重用需重建的口径在MQ/MQConsumer实例层）；
	// MQ.shutdown()为终态强制全停，此后addRef明确报错。
	// MasterAgent的RPC轮（createMQ/openMQ/subscribe）都发生在引用持有期间（客户端构造先addRef
	// 后调用），归零时无在飞轮，无需MQAgent那样的排空等待。
	private final Object lifecycleLock = new Object();
	private int refs; // 持有方计数：MQ实例+MQConsumer实例（构造addRef，close/构造失败release）
	private boolean idleStopped; // 归零已停connector（可随新引用复活；startAndWaitConnectionReady会重启）
	private volatile boolean terminated; // MQ.shutdown()终态

	public MasterAgent(Config config) {
		service = new Service(config);
		this.createPartitionHandle = null;
		this.deletePartitionHandle = null;
		RegisterProtocols(service);
	}

	public MasterAgent(Config config, Service service, ProtocolHandle<CreatePartition> createPartitionHandle,
					   ProtocolHandle<DeletePartition> deletePartitionHandle) {
		this.service = service;
		this.createPartitionHandle = createPartitionHandle;
		this.deletePartitionHandle = deletePartitionHandle;
		RegisterProtocols(this.service);
	}

	public void startAndWaitConnectionReady() {
		try {
			service.start();
			service.getConfig().forEachConnector(Connector::WaitReady);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void stop() {
		try {
			service.stop();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/** 【GB-D04】取一个引用；MQ.shutdown()后明确报错。MQ/MQConsumer构造（经MQ.clientAddRefs）调用。 */
	public void addRef() {
		synchronized (lifecycleLock) {
			if (terminated)
				throw new IllegalStateException("MasterAgent has been shutdown (MQ.shutdown());"
						+ " MQ client is terminated for this process, restart required");
			if (++refs == 1)
				idleStopped = false;
		}
	}

	/** 【GB-D04】释放一个引用；归零时停connector重连（无在飞轮可等，见字段区注释）。 */
	public void release() {
		synchronized (lifecycleLock) {
			if (--refs > 0)
				return;
			if (refs < 0) {
				refs = 0; // 防御：多余的release钳回0
				return;
			}
			if (terminated)
				return;
			// 只停connector不stop整个service：保持复活路径（startAndWaitConnectionReady重启）可用。
			service.getConfig().forEachConnector(Connector::stop);
			idleStopped = true;
		}
	}

	/** 【GB-D04】强制全停（MQ.shutdown()调用，不等引用归零）：停service（含connector重连+socket）。幂等。 */
	public void shutdown() {
		synchronized (lifecycleLock) {
			terminated = true;
		}
		stop();
	}

	/** 诊断/测试：当前引用计数。 */
	public int getRefs() {
		synchronized (lifecycleLock) {
			return refs;
		}
	}

	/** 诊断/测试：是否处于归零停机（connector已停，可随新引用复活）。 */
	public boolean isIdleStopped() {
		synchronized (lifecycleLock) {
			return idleStopped;
		}
	}

	/** 诊断/测试：是否已被MQ.shutdown()强制停机（终态）。 */
	public boolean isTerminated() {
		return terminated;
	}

	@Override
	protected long ProcessCreatePartitionRequest(CreatePartition r) throws Exception {
		if (null == this.createPartitionHandle)
			return Procedure.NotImplement;
		return this.createPartitionHandle.handle(r);
	}

	@Override
	protected long ProcessDeletePartitionRequest(DeletePartition r) throws Exception {
		if (null == this.deletePartitionHandle)
			return Procedure.NotImplement;
		return this.deletePartitionHandle.handle(r);
	}

	public static class Service extends Zeze.Net.Service {
		public Service(Config config) {
			super(eServiceName, config);
		}

		@Override
		public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
			super.OnHandshakeDone(so);
			// 只对连向Master的连接（connector方向）触发；此时socket已完成注册可安全发送协议。
			if (null != so.getConnector())
				OnMasterConnected(so);
		}

		/**
		 * 连向Master的连接（含断线重连）建立完成。在IO线程回调，实现不得同步等待rpc。
		 * Master重启即丢失managers注册表，子类需在此重发Register（Master端按host:port幂等）。
		 */
		protected void OnMasterConnected(@SuppressWarnings("unused") @NotNull AsyncSocket so) throws Exception {
		}
	}

	public BMQServers.Data createMQ(String topic, int partition, BOptions.Data options) {
		var r = new CreateMQ();
		r.Argument.setTopic(topic);
		r.Argument.setPartition(partition);
		if (null != options)
			r.Argument.setOptions(options);
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("openMQ error=" + IModule.getErrorCode(r.getResultCode()));
		return r.Result;
	}

	public BMQServers.Data openMQ(String topic) {
		var r = new OpenMQ();
		r.Argument.setTopic(topic);
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("openMQ error=" + IModule.getErrorCode(r.getResultCode()));
		return r.Result;
	}

	public BMQServers.Data subscribe(String topic) {
		var r = new Subscribe();
		r.Argument.setTopic(topic);
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("subscribe error=" + IModule.getErrorCode(r.getResultCode()));
		return r.Result;
	}

	// 【GB-D05】Register 携带 Manager 稳定身份（持久化于 Manager home 的自铸 id）：
	// Master 除幂等替换注册条目外，按 id 联动重写 mqTable 路由（换地址重注册→路由自愈）。
	public void register(String host, int port, int queueCount, long managerId) {
		var r = new Register();
		r.Argument.setHost(host);
		r.Argument.setPort(port);
		r.Argument.setPartitionIndex(queueCount); // WARNING 这里使用了这个变量的意思是这个manager的队列数量。
		r.Argument.setManagerId(managerId);

		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("register error=" + IModule.getErrorCode(r.getResultCode()));
	}

	// 【GB-D01】周期上报本地分区清单（磁盘真相），Master 与 mqTable 对账（孤儿超宽限期下发删除）。
	public void reportPartitions(BReportPartitions.Data report) {
		var r = new ReportPartitions();
		r.Argument.getTopics().addAll(report.getTopics());
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("reportPartitions error=" + IModule.getErrorCode(r.getResultCode()));
	}

	public void reportLoad(double load) {
		var r = new ReportLoad();
		r.Argument.setLoad(load);
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("reportLoad error=" + IModule.getErrorCode(r.getResultCode()));
	}
}
