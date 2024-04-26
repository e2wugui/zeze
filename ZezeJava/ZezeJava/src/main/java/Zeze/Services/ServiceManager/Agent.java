package Zeze.Services.ServiceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Component.Threading;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Net.Service.ProtocolFactoryHandle;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Action1;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class Agent extends AbstractAgent {
	static final Logger logger = LogManager.getLogger(Agent.class);

	/**
	 * 使用Config配置连接信息，可以配置是否支持重连。
	 * 用于测试：Agent.Client.NewClientSocket(...)，不会自动重连，不要和Config混用。
	 */
	public static final String defaultServiceName = "Zeze.Services.ServiceManager.Agent";

	private final AgentClient client;
	private final ConcurrentHashMap<BServiceInfo, BServiceInfo> registers = new ConcurrentHashMap<>();

	private Threading threading;

	public AgentClient getClient() {
		return client;
	}

	@Override
	public void start() throws Exception {
		client.start();
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
	public void subscribeServicesAsync(@NotNull BSubscribeArgument infos, @Nullable Action1<List<SubscribeState>> action) {
		waitConnectorReady();

		var r = new Subscribe(infos);
		r.Send(client.getSocket(), __ -> {
			var states = new ArrayList<SubscribeState>();
			for (var info : r.Argument.subs) {
				var state = subscribeStates.computeIfAbsent(info.getServiceName(), (key) -> new SubscribeState(info));
				states.add(state);
				var result = r.Result.map.get(info.getServiceName());
				if (null != result)
					state.onFirstCommit(result);
			}
			if (null != action) {
				try {
					action.run(states);
				} catch (Exception ex) {
					logger.warn("", ex);
				}
			}
			return 0;
		});
		logger.debug("subscribeServicesAsync {}", infos);
	}

	@Override
	public @NotNull SubscribeState subscribeService(@NotNull BSubscribeInfo info) {
		waitConnectorReady();
		return super.subscribeService(info);
	}

	@Override
	public void unSubscribeService(@NotNull BUnSubscribeArgument arg) {
		waitConnectorReady();

		var r = new UnSubscribe(arg);
		r.SendAndWaitCheckResultCode(client.getSocket());
		logger.debug("UnSubscribeService {}", arg);
		for (var serviceName : arg.serviceNames)
			subscribeStates.remove(serviceName);
	}

	@Override
	public void offlineRegister(@NotNull BOfflineNotify argument, @NotNull Action1<BOfflineNotify> handle) {
		waitConnectorReady();
		onOfflineNotifies.putIfAbsent(argument.notifyId, handle);
		new OfflineRegister(argument).SendAndWaitCheckResultCode(client.getSocket());
	}

	@Override
	public boolean setServerLoad(@NotNull BServerLoad load) {
		return new SetServerLoad(load).Send(client.getSocket());
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

		subscribeServicesAsync(subArg, null);
	}

	private long processEdit(EditService r) {

		for (var it = r.Argument.getRemove().iterator(); it.hasNext(); /**/) {
			var unReg = it.next();
			var state = subscribeStates.get(unReg.getServiceName());
			if (null == state || !state.onUnRegister(unReg))
				it.remove();
		}

		for (var reg : r.Argument.getAdd()) {
			var state = subscribeStates.get(reg.getServiceName());
			if (null == state)
				continue; // 忽略本地没有订阅的。最好加个日志。
			state.onRegister(reg);
		}

		r.SendResult();
		triggerOnChanged(r.Argument);
		return 0;
	}

	private long processKeepAlive(KeepAlive r) {
		if (onKeepAlive != null)
			Task.getCriticalThreadPool().execute(onKeepAlive);
		r.SendResultCode(KeepAlive.Success);
		return Procedure.Success;
	}

	private long processSetServerLoad(SetServerLoad setServerLoad) {
		loads.put(setServerLoad.Argument.getName(), setServerLoad.Argument);
		if (onSetServerLoad != null) {
			Task.getCriticalThreadPool().execute(() -> {
				try {
					onSetServerLoad.run(setServerLoad.Argument);
				} catch (Throwable e) { // logger.error
					// run handle.
					logger.error("", e);
				}
			});
		}
		return Procedure.Success;
	}

	private long processOfflineNotify(OfflineNotify r) {
		try {
			if (triggerOfflineNotify(r.Argument)) {
				r.SendResult();
				return 0;
			}
			r.trySendResultCode(2);
		} catch (Throwable ignored) { // ignored
			// rpc response any.
			r.trySendResultCode(3);
		}
		return 0;
	}

	public Agent(Config config) {
		this(config, null);
	}

	public Agent(Config config, String netServiceName) {
		super.config = config;

		client = (null == netServiceName || netServiceName.isEmpty())
				? new AgentClient(this, config)
				: new AgentClient(this, config, netServiceName);

		client.AddFactoryHandle(EditService.TypeId_, new ProtocolFactoryHandle<>(
				EditService::new, this::processEdit, TransactionLevel.None, DispatchMode.Direct));
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

		client.AddFactoryHandle(OfflineNotify.TypeId_, new ProtocolFactoryHandle<>(
				OfflineNotify::new, this::processOfflineNotify, TransactionLevel.None, DispatchMode.Critical));
		client.AddFactoryHandle(OfflineRegister.TypeId_, new ProtocolFactoryHandle<>(
				OfflineRegister::new, null, TransactionLevel.None, DispatchMode.Normal));
		client.AddFactoryHandle(NormalClose.TypeId_, new ProtocolFactoryHandle<>(
				NormalClose::new, null, TransactionLevel.None, DispatchMode.Critical));

		threading = new Threading(client, config.getServerId());
		threading.RegisterProtocols(client);
	}

	@Override
	public Threading getThreading() {
		return threading;
	}

	public void stop() throws Exception {
		lock();
		try {
			if (client != null) {
				var so = client.getSocket();
				if (so != null) // 有可能提前关闭,so==null时执行下面这行会抛异常
					new NormalClose().SendAndWaitCheckResultCode(so);
				client.stop();
			}
			if (null != threading) {
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
