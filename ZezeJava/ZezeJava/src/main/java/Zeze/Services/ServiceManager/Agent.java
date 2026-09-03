package Zeze.Services.ServiceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Component.Threading;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Net.Service.ProtocolFactoryHandle;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.OutInt;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Agent extends AbstractAgent {
	static final @NotNull Logger logger = LogManager.getLogger(Agent.class);

	/**
	 * 使用Config配置连接信息，可以配置是否支持重连。
	 * 用于测试：Agent.Client.NewClientSocket(...)，不会自动重连，不要和Config混用。
	 */
	public static final String defaultServiceName = "Zeze.Services.ServiceManager.Agent";

	private final @NotNull AgentClient client;
	private final ConcurrentHashMap<BServiceInfo, BServiceInfo> registers = new ConcurrentHashMap<>();

	private Threading threading;

	public @NotNull AgentClient getClient() {
		return client;
	}

	@Override
	public void start() throws Exception {
		lock();
		try {
			client.start();
			if (tid128UdpClient != null)
				tid128UdpClient.start();
		} finally {
			unlock();
		}
	}

	@Override
	public void waitReady() {
		waitConnectorReady();
	}

	public void waitConnectorReady() {
		// 实际上只有一个连接，这样就不用查找了。
		client.getConfig().forEachConnector(Connector::WaitReady);
	}

	@Override
	public void editService(@NotNull BEditService arg) {
		for (var info : arg.getAdd())
			verify(info.getServiceIdentity());
		waitConnectorReady();

		var edit = new EditService(arg);
		edit.SendAndWaitCheckResultCode(client.getSocket());

		// 成功以后更新本地信息。
		for (var unReg : arg.getRemove())
			registers.remove(unReg);

		for (var reg : arg.getAdd())
			registers.put(reg, reg);
	}

	@Override
	public @NotNull CompletableFuture<List<SubscribeState>> subscribeServicesAsync(@NotNull BSubscribeArgument infos) {
		waitConnectorReady();
		logger.debug("subscribeServicesAsync: {}", infos);
		var cf = new CompletableFuture<List<SubscribeState>>();
		if (!new Subscribe(infos).Send(client.getSocket(), r -> {
			var rc = r.getResultCode();
			if (rc == 0) {
				var edits = new BEditService();
				var states = new ArrayList<SubscribeState>(r.Argument.subs.size());
				for (var info : r.Argument.subs) {
					var state = subscribeStates.computeIfAbsent(info.getServiceName(), __ -> new SubscribeState(info));
					state.updateSubscribeInfo(info); // 同名重订阅同步过滤版本（FND-S2-8），防重连重放回退
					states.add(state);
					var result = r.Result.map.get(info.getServiceName());
					if (result != null)
						state.onFirstCommit(result, edits);
				}
				try {
					triggerOnChanged(edits);
				} catch (Throwable e) { // logger.error
					logger.error("subscribeServicesAsync: triggerOnChanged exception:", e);
				}
				cf.complete(states);
			} else {
				logger.error("subscribeServicesAsync: resultCode={}", rc);
				cf.completeExceptionally(new IllegalStateException("Subscribe resultCode=" + rc));
			}
			return 0;
		})) {
			logger.error("subscribeServicesAsync: send Subscribe failed");
			cf.completeExceptionally(new IllegalStateException("send Subscribe failed"));
		}
		return cf;
	}

	@Override
	public @NotNull SubscribeState subscribeService(@NotNull BSubscribeInfo info) {
		waitConnectorReady();
		return super.subscribeService(info);
	}

	@Override
	public void unSubscribeService(@NotNull BUnSubscribeArgument arg) {
		waitConnectorReady();
		new UnSubscribe(arg).SendAndWaitCheckResultCode(client.getSocket());
		logger.debug("unSubscribeService: {}", arg);
		for (var serviceName : arg.serviceNames)
			subscribeStates.remove(serviceName);
	}

	@Override
	public boolean setServerLoad(@NotNull BServerLoad load) {
		return new SetServerLoad(load).Send(client.getSocket());
	}

	@Override
	protected boolean allocateAsync(@NotNull String globalName, int allocCount,
									@NotNull ProtocolHandle<Rpc<BAllocateIdArgument, BAllocateIdResult>> callback) {
		if (allocCount < 1)
			throw new IllegalArgumentException();
		var r = new AllocateId();
		r.Argument.setName(globalName);
		r.Argument.setCount(allocCount);
		return r.Send(client.getSocket(), callback);
	}

	@Override
	protected void allocate(@NotNull AutoKey autoKey, int pool) {
		if (pool < 1)
			throw new IllegalArgumentException();
		var r = new AllocateId();
		r.Argument.setName(autoKey.getName());
		r.Argument.setCount(pool);
		r.SendAndWaitCheckResultCode(client.getSocket());
		autoKey.setCurrentAndCount(r.Result.getStartId(), r.Result.getCount());
	}

	public void onConnected() {
		// 先上报Identify：SM据此把serverId记在会话上，断线时广播Suspect。
		// 重连重发=恢复提示资格（与正确性无关，正确性由租约裁决）。
		try {
			var identify = new Identify();
			identify.Argument.serverId = config.getServerId();
			identify.Send(client.getSocket());
		} catch (Throwable ex) { // logger.debug
			logger.debug("OnConnected.Identify", ex);
		}

		var edit = new BEditService();
		edit.getAdd().addAll(registers.keySet());
		try {
			editService(edit);
		} catch (Throwable ex) { // logger.debug
			// skip and continue.
			logger.debug("OnConnected.Register", ex);
		}

		var subArg = new BSubscribeArgument();
		for (var e : subscribeStates.values())
			subArg.subs.add(e.getSubscribeInfo());

		subscribeServicesAsync(subArg);
	}

	private long processEditService(@NotNull EditService r) {
		for (var it = r.Argument.getRemove().iterator(); it.hasNext(); ) {
			var unReg = it.next();
			var state = subscribeStates.get(unReg.getServiceName());
			if (null == state || !state.onUnRegister(unReg))
				it.remove();
		}

		// 触发回调前修正集合之间的关系。
		// 删除后来又加入的。
		r.Argument.getRemove().removeIf(r.Argument.getAdd()::contains);

		for (var reg : r.Argument.getAdd()) {
			var state = subscribeStates.get(reg.getServiceName());
			if (null == state)
				continue; // 忽略本地没有订阅的。最好加个日志。
			var oldNotSame = state.onRegister(reg);
			if (null != oldNotSame)
				r.Argument.getRemove().add(oldNotSame);
		}

		r.SendResult();
		try {
			triggerOnChanged(r.Argument);
		} catch (Throwable e) { // logger.error
			logger.error("processEditService: triggerOnChanged exception:", e);
		}
		return 0;
	}

	private long processKeepAlive(@NotNull KeepAlive r) {
		if (onKeepAlive != null)
			Task.getCriticalThreadPool().execute(onKeepAlive);
		r.SendResultCode(KeepAlive.Success);
		return Procedure.Success;
	}

	private long processSetServerLoad(@NotNull SetServerLoad setServerLoad) {
		loads.put(setServerLoad.Argument.getName(), setServerLoad.Argument);
		if (onSetServerLoad != null) {
			Task.getCriticalThreadPool().execute(() -> {
				try {
					if (onSetServerLoad != null) {
						onSetServerLoad.run(setServerLoad.Argument);
					}
				} catch (Throwable e) { // logger.error
					// run handle.
					logger.error("", e);
				}
			});
		}
		return Procedure.Success;
	}

	// Suspect仅是提示：转化为onSuspect回调（应用接takeover.tryTransfer），
	// 租约未过期时tryTransfer内部安排到过期时刻精确重试，不会误接管。
	private long processSuspect(@NotNull Suspect r) {
		var on = onSuspect;
		if (on != null) {
			try {
				on.run(r.Argument.serverId);
			} catch (Throwable e) { // logger.error
				logger.error("processSuspect serverId=" + r.Argument.serverId, e);
			}
		}
		return 0;
	}

	public Agent(@NotNull Config config) throws Exception {
		this(config, null);
	}

	public Agent(@NotNull Config config, @Nullable String netServiceName) throws Exception {
		super.config = config;

		client = (null == netServiceName || netServiceName.isEmpty())
				? new AgentClient(this, config)
				: new AgentClient(this, config, netServiceName);

		client.AddFactoryHandle(EditService.TypeId_, new ProtocolFactoryHandle<>(
				EditService::new, this::processEditService, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(Subscribe.TypeId_, new ProtocolFactoryHandle<>(
				Subscribe::new, null, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(UnSubscribe.TypeId_, new ProtocolFactoryHandle<>(
				UnSubscribe::new, null, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(KeepAlive.TypeId_, new ProtocolFactoryHandle<>(
				KeepAlive::new, this::processKeepAlive, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(AllocateId.TypeId_, new ProtocolFactoryHandle<>(
				AllocateId::new, null, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(SetServerLoad.TypeId_, new ProtocolFactoryHandle<>(
				SetServerLoad::new, this::processSetServerLoad, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(Suspect.TypeId_, new ProtocolFactoryHandle<>(
				Suspect::new, this::processSuspect, TransactionLevel.None, DispatchMode.Critical));

		threading = new Threading(client, config.getServerId());
		threading.RegisterProtocols(client);

		// todo 使用老的协议得到服务器的Id128UdpServer的port。
		//  现在的方式使用和tcp.port一样.
		// 查找smAgent的Service，使用其中第一个Connector的信息。
		var outIp = new OutObject<String>();
		var outPort = new OutInt();
		client.getConfig().forEachConnector2(connector -> {
			outIp.value = connector.getHostNameOrAddress();
			outPort.value = connector.getPort();
			return false;
		});
		if (outIp.value != null && outPort.value > 0) // client需要
			super.tid128UdpClient = new Id128UdpClient(this, outIp.value, outPort.value, client::nextSessionId);
	}

	@Override
	public @NotNull Threading getThreading() {
		return threading;
	}

	public void stop() throws Exception {
		lock();
		try {
			if (tid128UdpClient != null) {
				tid128UdpClient.stop();
				tid128UdpClient = null;
			}
			client.stop();
			if (threading != null) {
				threading.close();
				threading = null;
			}
		} finally {
			unlock();
		}
	}

	@Override
	public void close() throws IOException {
		try {
			stop();
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
