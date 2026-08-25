package Zeze.Arch;

import Zeze.Builtin.ProviderDirect.BLoginKey;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Transaction.Transaction;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

/** 单登录端点发送器：在 OnlineSpec 之上追加仅对单端点成立的能力（response/link 直发）。 */
public final class LoginOnlineSpec extends OnlineSpec {
	private final @NotNull String account;
	private final @NotNull String clientId;

	LoginOnlineSpec(@NotNull Online online, @NotNull String account, @NotNull String clientId) {
		super(online, new OnlineTarget.Login(account, clientId));
		this.account = account;
		this.clientId = clientId;
	}

	@Override
	public @NotNull LoginOnlineSpec trying(boolean trying) { // 协变返回，保持链式
		super.trying(trying);
		return this;
	}

	/** 发送 rpc 响应（事务感知）。 */
	public void sendResponse(@NotNull Rpc<?, ?> r) {
		r.setRequest(false);
		send(r); // 走 send 族：非 request 放行守卫
	}

	/** 直接通过 link 发送（事务感知）；忽略 trying 选项。 */
	public void send(@NotNull String linkName, long linkSid, @NotNull Protocol<?> p) {
		var ol = online; // 字段读进局部变量：闭包不捕获 spec 实例
		var key = new BLoginKey(account, clientId);
		Task.runTxnAware(() -> ol.send(key, linkName, linkSid, p));
	}

	/** 事务回滚时直接通过 link 发送；选项规则同上。 */
	public void sendWhileRollback(@NotNull String linkName, long linkSid, @NotNull Protocol<?> p) {
		var ol = online;
		var key = new BLoginKey(account, clientId);
		Transaction.whileRollback(() -> ol.send(key, linkName, linkSid, p));
	}
}
