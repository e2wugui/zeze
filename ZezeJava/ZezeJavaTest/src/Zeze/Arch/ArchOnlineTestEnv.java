package Zeze.Arch;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Builtin.Online.BLink;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Transaction.Procedure;
import Zeze.Util.FuncLong;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Real Arch.Online tables and procedures; only the link transport is replaced. */
final class ArchOnlineTestEnv extends AppBase implements AutoCloseable {

	static final String ACCOUNT = "online-account";
	static final String CLIENT_ID = "online-client";
	static final String LINK_NAME = "online-test-link";

	final Application zeze;
	final Online online;
	final SinkSocket socket;

	ArchOnlineTestEnv(int serverId) throws Exception {
		Task.tryInitThreadPool();
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(serverId);
		conf.setTakeoverMode("off");
		conf.setDefaultTableConf(new Config.TableConf());
		var database = new Config.DatabaseConf();
		database.setDatabaseUrl("arch_online_behavior_" + serverId);
		conf.getDatabaseConfMap().put("", database);
		zeze = new Application("ArchOnlineTest" + serverId, conf);
		new ProviderApp(zeze);
		online = new Online(this);
		socket = new SinkSocket(online.providerApp.providerService);
		zeze.initialize(this);
		zeze.start();
	}

	@Override
	public Application getZeze() {
		return zeze;
	}

	@Override
	public void close() throws Exception {
		zeze.stop();
	}

	void run(String name, FuncLong action) {
		assertEquals(Procedure.Success, zeze.newProcedure(action, name).call());
	}

	void seedLogin(long loginVersion, long confirmIndex, long notifyIndex) {
		run("seedOnlineLogin", () -> {
			var row = online.getOrAddOnline(ACCOUNT);
			row.setAccount(ACCOUNT);
			row.setLastLoginVersion(loginVersion);
			var login = row.getLogins().getOrAdd(CLIENT_ID);
			login.setLoginVersion(loginVersion);
			login.setServerId(zeze.getConfig().getServerId());
			login.setLink(new BLink(LINK_NAME, 1L, AbstractOnline.eLogined));
			login.setReliableNotifyConfirmIndex(confirmIndex);
			login.setReliableNotifyIndex(notifyIndex);
			var local = online._tlocal.getOrAdd(ACCOUNT).getLogins().getOrAdd(CLIENT_ID);
			local.setLoginVersion(loginVersion);
			return Procedure.Success;
		});
	}

	Session session(long linkSid, String context) {
		var dispatch = new Dispatch();
		dispatch.setSender(socket);
		dispatch.Argument.setAccount(ACCOUNT);
		dispatch.Argument.setLinkSid(linkSid);
		dispatch.Argument.setContext(context);
		return new Session(dispatch);
	}

	<P extends Protocol<?>> P attach(P rpc, Session session) {
		rpc.setSender(socket);
		rpc.setUserState(session);
		return rpc;
	}

	static final class Session extends ProviderUserSession {
		final List<Protocol<?>> replies = new ArrayList<>();

		Session(Dispatch dispatch) {
			super(dispatch);
		}

		@Override
		public @NotNull String getLinkName() {
			return LINK_NAME;
		}

		@Override
		public void respond(@NotNull Protocol<?> protocol) {
			Task.runTxnAware(() -> replies.add(protocol));
		}
	}

	private static final class SinkSocket extends AsyncSocket {
		SinkSocket(ProviderService service) {
			super(service);
		}

		@Override
		public Type getType() {
			return Type.eClient;
		}

		@Override
		public boolean Send(byte @NotNull [] bytes, int offset, int length) {
			return true;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		protected void doClose(@Nullable Throwable error, boolean gracefully) {
		}
	}
}
