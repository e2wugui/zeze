package Zeze.Arch;

import java.util.Map;
import Zeze.Arch.Beans.BSend;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Builtin.Provider.Send;
import Zeze.Builtin.ProviderDirect.BLoginKey;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Transaction;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 用户登录会话。
 * 记录账号，roleId，LinkName，SessionId等信息。
 */
public class ProviderUserSession {
	private static final Logger logger = LogManager.getLogger(ProviderUserSession.class);

	protected final @NotNull Dispatch dispatch;

	public ProviderUserSession(@NotNull Dispatch dispatch) {
		this.dispatch = dispatch;
	}

	public void kick(int code, @NotNull String desc) {
		ProviderImplement.sendKick(getLink(), getLinkSid(), code, desc);
	}

	public @NotNull ProviderService getService() {
		return (ProviderService)dispatch.getSender().getService();
	}

	public @NotNull String getAccount() {
		return dispatch.Argument.getAccount();
	}

	public @NotNull String getContext() {
		return dispatch.Argument.getContext();
	}

	public @NotNull String getOnlineSetName() {
		return dispatch.Argument.getOnlineSetName();
	}

	public boolean isLogin() {
		// 全仓约定 context 非空 == 已登录（getRoleId、LinkdProvider 广播、ProviderWithOnline.LinkBroken、
		// sendOnline 同向）；Arch.Online.ProcessLogoutRequest 是唯一调用点，反转使已登录用户的 Logout 永远被拒。
		return !getContext().isEmpty();
	}

	public @Nullable Long getRoleId() {
		var context = getContext();
		if (context.isEmpty())
			return null;
		try {
			return Long.parseLong(context);
		} catch (NumberFormatException e) {
			return null; // 账号在线模式下context是clientId(任意字符串,见Arch.Online登录setContext)，没有roleId
		}
	}

	public long getRoleIdNotNull() {
		var roleId = getRoleId();
		if (roleId == null)
			throw new RuntimeException("roleId is null");
		return roleId;
	}

	public long getLinkSid() {
		return dispatch.Argument.getLinkSid();
	}

	public @NotNull String getLinkName() {
		return ProviderService.getLinkName(getLink());
	}

	public AsyncSocket getLink() {
		return dispatch.getSender();
	}

	// ---------------- respond 家族 ----------------
	// 词汇表与 OnlineSpec 同构：无标记=事务感知（运行中的事务内延迟到 commit 发送，回滚不发；无事务立即发送）；
	// Now=立即发送；WhileRollback=事务回滚时发送。除 fireAndForget 外均经 Online 失败记账（onSendError）触发下线流程。

	/** 事务感知发送响应/推送：运行中的事务内延迟到 commit 发送（回滚不发），否则立即发送。 */
	public void respond(@NotNull Protocol<?> p) {
		Task.runTxnAware(() -> respondNow(p));
	}

	/** 立即发送，不等事务提交（即使在事务内；之后 rollback/redo 无法撤销）。 */
	public void respondNow(@NotNull Protocol<?> p) {
		p.setRequest(false);
		protocolLogSend(p);
		sendAccounted(p.getTypeId(), new Binary(p.encode()));
	}

	/** 事务回滚时发送。要求当前存在运行中的事务。 */
	public void respondWhileRollback(@NotNull Protocol<?> p) {
		Transaction.whileRollback(() -> respondNow(p));
	}

	/**
	 * 事务感知发送 rpc 响应，fire-and-forget：绕过 Online 失败记账，发送失败静默丢弃，不触发 onSendError 下线流程。
	 * 下线感知主路径是 LinkBroken 协议，记账只是加速/兜底。适用：Rpc 响应 ack、高频推送等不需要失败追责的场景。
	 */
	public void respondFireAndForget(@NotNull Rpc<?, ?> rpc) {
		Task.runTxnAware(() -> sendFireAndForgetReal(rpc));
	}

	protected void sendFireAndForgetReal(@NotNull Rpc<?, ?> rpc) {
		rpc.setRequest(false);
		protocolLogSend(rpc);
		var send = new Send(new BSend(rpc.getTypeId(), new Binary(rpc.encode())));
		send.Argument.getLinkSids().add(getLinkSid());

		var link = getLink();
		if (link != null && !link.isClosed()) {
			send.Send(link);
			return;
		}
		// 可能发生了重连，尝试再次查找发送。网络断开以后，linkSid已经不可靠了，先这样写着吧。
		var connector = getService().getLinks().get(getLinkName());
		if (connector != null && connector.isHandshakeDone()) {
			dispatch.setSender(link = connector.getSocket());
			send.Send(link);
		}
	}

	/** @deprecated 使用 {@link #respondFireAndForget(Rpc)} 替代（语义不变：事务感知 + 绕过 Online 失败记账）。 */
	@Deprecated
	public void sendResponseDirect(@NotNull Rpc<?, ?> rpc) {
		var t = Transaction.getCurrent();
		if (t != null)
			t.runWhileCommit(() -> sendFireAndForgetReal(rpc));
		else
			sendFireAndForgetReal(rpc);
	}

	/** @deprecated 使用 {@link #respondNow(Protocol)}（内部自行编码）或 respond 家族替代。 */
	@Deprecated
	public void sendResponse(@NotNull Binary fullEncodedProtocol) {
		var bytes = fullEncodedProtocol.bytesUnsafe();
		var offset = fullEncodedProtocol.getOffset();
		var moduleId = ByteBuffer.ToInt(bytes, offset);
		var protocolId = ByteBuffer.ToInt(bytes, offset + 4);
		sendResponse(Protocol.makeTypeId(moduleId, protocolId), fullEncodedProtocol);
	}

	protected boolean sendOnline(AsyncSocket link, @NotNull Send send) {
		var providerImpl = getService().providerApp.providerImplement;
		if (providerImpl instanceof Zeze.Game.ProviderWithOnline gameProvider) {
			var roleId = getRoleId();
			if (roleId != null) {
				var name = dispatch.Argument.getOnlineSetName();
				var online = gameProvider.getOnline(name);
				if (online != null)
					return online.send(link, Map.of(getLinkSid(), roleId), send);
				logger.error("unknown onlineSetName: {}", name);
			}
			// 没有登录的会话不需要转给Online处理。转给Online是为了处理发送失败的结果。
			// 这种情况下，忽略发送结果。
			return send.Send(link);
		}
		if (providerImpl instanceof Zeze.Arch.ProviderWithOnline archProvider) {
			var online = archProvider.getOnline();
			var context = getContext();
			var loginKey = new BLoginKey(getAccount(), context);
			if (!context.isEmpty())
				return online.send(link, Map.of(getLinkSid(), loginKey), send);
			// 没有登录的会话不需要转给Online处理。转给Online是为了处理发送失败的结果。
			// 这种情况下，忽略发送结果。
			return send.Send(link);
		}
		return send.Send(link);
	}

	protected boolean sendAccounted(long typeId, @NotNull Binary fullEncodedProtocol) {
		var send = new Send(new BSend(typeId, fullEncodedProtocol));
		send.Argument.getLinkSids().add(getLinkSid());

		var link = getLink();
		if (link != null && !link.isClosed())
			return sendOnline(link, send);
		// 可能发生了重连，尝试再次查找发送。网络断开以后，linkSid已经不可靠了，先这样写着吧。
		var connector = getService().getLinks().get(getLinkName());
		if (connector != null && connector.isHandshakeDone()) {
			dispatch.setSender(link = connector.getSocket());
			return sendOnline(link, send);
		}
		return false;
	}

	/** @deprecated 使用 {@link #respondNow(Protocol)}（内部自行编码）或 respond 家族替代。 */
	@Deprecated
	public boolean sendResponse(long typeId, @NotNull Binary fullEncodedProtocol) {
		return sendAccounted(typeId, fullEncodedProtocol);
	}

	private void protocolLogSend(@NotNull Protocol<?> p) {
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(p.getTypeId())) {
			var roleId = getRoleId();
			if (roleId == null)
				roleId = -getLinkSid();
			AsyncSocket.log("Send", roleId, dispatch.Argument.getOnlineSetName(), p);
		}
	}

	/**
	 * onError 钩子配套：仅当 p 是尚未回复的 Rpc 请求时，以 resultCode 立即回错误响应
	 * （Now：不等外层事务）；handler 已回复过或非 Rpc 则静默跳过。
	 */
	public void tryRespondErrorNow(@NotNull Protocol<?> p, long resultCode) {
		if (p instanceof Rpc && p.isRequest()) {
			p.setResultCode(resultCode);
			respondNow(p);
		}
	}

	/** @deprecated 使用 {@link #tryRespondErrorNow(Protocol, long)} 替代。 */
	@Deprecated
	public void trySendResponse(@NotNull Protocol<?> p, long resultCode) {
		tryRespondErrorNow(p, resultCode);
	}

	/**
	 * @deprecated 立即发送语义不变，使用 {@link #respondNow(Protocol)} 替代；事务感知请用 {@link #respond(Protocol)}。
	 */
	@Deprecated
	public void sendResponse(@NotNull Protocol<?> p) {
		p.setRequest(false);
		protocolLogSend(p);
		sendResponse(p.getTypeId(), new Binary(p.encode()));
	}

	/** @deprecated 使用 {@link #respond(Protocol)} 替代（事务感知：无事务时立即发送，不再要求事务存在）。 */
	@Deprecated
	public void sendResponseWhileCommit(long typeId, @NotNull Binary fullEncodedProtocol) {
		Transaction.whileCommit(() -> sendResponse(typeId, fullEncodedProtocol));
	}

	/** @deprecated 使用 {@link #respond(Protocol)} 替代（事务感知：无事务时立即发送，不再要求事务存在）。 */
	@Deprecated
	public void sendResponseWhileCommit(@NotNull Binary fullEncodedProtocol) {
		Transaction.whileCommit(() -> sendResponse(fullEncodedProtocol));
	}

	/** @deprecated 使用 {@link #respond(Protocol)} 替代（事务感知：无事务时立即发送，不再要求事务存在）。 */
	@Deprecated
	public void sendResponseWhileCommit(@NotNull Protocol<?> p) {
		Transaction.whileCommit(() -> sendResponse(p));
	}

	// 这个方法用来优化广播协议。不能用于Rpc，先隐藏。
	@SuppressWarnings("unused")
	@Deprecated
	protected void sendResponseWhileRollback(long typeId, @NotNull Binary fullEncodedProtocol) {
		Transaction.whileRollback(() -> sendResponse(typeId, fullEncodedProtocol));
	}

	@SuppressWarnings("unused")
	@Deprecated
	protected void sendResponseWhileRollback(@NotNull Binary fullEncodedProtocol) {
		Transaction.whileRollback(() -> sendResponse(fullEncodedProtocol));
	}

	/** @deprecated 使用 {@link #respondWhileRollback(Protocol)} 替代。 */
	@Deprecated
	public void sendResponseWhileRollback(@NotNull Protocol<?> p) {
		Transaction.whileRollback(() -> sendResponse(p));
	}

	public static @NotNull ProviderUserSession get(@NotNull Protocol<?> context) {
		var state = context.getUserState();
		if (state == null)
			throw new IllegalStateException("not auth");
		return (ProviderUserSession)state;
	}

	@Override
	public String toString() {
		return "(account=" + dispatch.Argument.getAccount() + ",roleId=" + dispatch.Argument.getContext() + ")";
	}
}
