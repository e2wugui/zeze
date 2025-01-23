package Zeze.MQ.Master;

import Zeze.Builtin.MQ.Master.CreateMQ;
import Zeze.Builtin.MQ.Master.CreatePartition;
import Zeze.Builtin.MQ.Master.ReportLoad;
import Zeze.Builtin.MQ.BOptions;
import Zeze.Builtin.MQ.Master.BMQServers;
import Zeze.Builtin.MQ.Master.OpenMQ;
import Zeze.Builtin.MQ.Master.Subscribe;
import Zeze.Builtin.MQ.Master.Register;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.Connector;
import Zeze.Net.ProtocolHandle;
import Zeze.Transaction.Procedure;

public class MasterAgent extends AbstractMasterAgent {
	public static final String eServiceName = "Zeze.MQ.Master.Agent";
	private final Service service;
	private final ProtocolHandle<CreatePartition> createPartitionHandle;

	public MasterAgent(Config config) {
		service = new Service(config);
		this.createPartitionHandle = null;
		RegisterProtocols(service);
	}

	public MasterAgent(Config config, Service service, ProtocolHandle<CreatePartition> createPartitionHandle) {
		this.service = service;
		this.createPartitionHandle = createPartitionHandle;
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

	@Override
	protected long ProcessCreatePartitionRequest(CreatePartition r) throws Exception {
		if (null == this.createPartitionHandle)
			return Procedure.NotImplement;
		return this.createPartitionHandle.handle(r);
	}

	public static class Service extends Zeze.Net.Service {
		public Service(Config config) {
			super(eServiceName, config);
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

	public void register(String host, int port, int queueCount) {
		var r = new Register();
		r.Argument.setHost(host);
		r.Argument.setPort(port);
		r.Argument.setPartitionIndex(queueCount); // WARNING 这里使用了这个变量的意思是这个manager的队列数量。

		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("register error=" + IModule.getErrorCode(r.getResultCode()));
	}

	public void reportLoad(double load) {
		var r = new ReportLoad();
		r.Argument.setLoad(load);
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("reportLoad error=" + IModule.getErrorCode(r.getResultCode()));
	}
}
