package Zeze.Services.Log4jQuery;

import java.util.concurrent.TimeUnit;
import Zeze.Builtin.LogService.BBrowse;
import Zeze.Builtin.LogService.BCondition;
import Zeze.Builtin.LogService.BResult;
import Zeze.Builtin.LogService.BSearch;
import Zeze.Builtin.LogService.Browse;
import Zeze.Builtin.LogService.CloseSession;
import Zeze.Builtin.LogService.NewSession;
import Zeze.Builtin.LogService.Search;
import Zeze.Net.Rpc;
import Zeze.Services.LogAgent;
import Zeze.Transaction.Procedure;
import Zeze.Util.TaskCompletionSource;

/**
 * 客户端日志查询会话：封装对单个日志服务端的 NewSession/Search/Browse/CloseSession RPC 生命周期。
 */
public class Session implements AutoCloseable {
	private final String serverName;
	private final LogAgent agent;
	private final long sessionId;

	public LogAgent getAgent() {
		return agent;
	}

	public String getName() {
		return serverName;
	}

	/**
	 * 目标日志服务器就绪 socket：未注册的 serverName（{@code __getLogServer} 未命中返回
	 * null）抛带名字的 IllegalArgumentException（对齐 {@link LogAgent#query} 的显式判空）
	 * ——裸解引用的 NPE 不满足会话级判别，调用方不可自愈。
	 */
	private Zeze.Net.AsyncSocket readySocket() {
		var connector = agent.__getLogServer(serverName);
		if (connector == null)
			throw new IllegalArgumentException("unknown log server: " + serverName);
		return connector.GetReadySocket();
	}

	public Session(LogAgent agent, String serverName, String logName) {
		this.agent = agent;
		this.serverName = serverName;
		var r = new NewSession();
		r.Argument.setLogName(logName);
		// 与browse/search/close同宽60s：服务端NewSession含惰性清理（逐会话锁）与索引装载等重活，
		// 默认5s在多会话/慢盘下超时即建会话失败。
		r.SendForWait(readySocket(), 60_000).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("error " + r.getResultCode());
		sessionId = r.Result.getId();
	}

	public TaskCompletionSource<BResult.Data> search(int limit, boolean reset,
													 BCondition.Data condition) {
		var r = new Search(new BSearch.Data(sessionId, limit, reset, condition));
		return checkResultCode(r, r.SendForWait(readySocket(), 60_000));
	}

	public TaskCompletionSource<BResult.Data> browse(int limit, float offsetFactor, boolean reset,
													 BCondition.Data condition) {
		var r = new Browse(new BBrowse.Data(sessionId, limit, offsetFactor, reset, condition));
		// 服务端扫描量级与search相同（beginTime=-1或索引缺失时全量线性扫），不能用RPC默认5s。
		return checkResultCode(r, r.SendForWait(readySocket(), 60_000));
	}

	/**
	 * search/browse 的应答 TCS 加 resultCode 检查。
	 * Zeze RPC 对非零 resultCode 的应答也<b>正常完成</b> future（resultCode 只是 rpc 对象字段，
	 * Rpc.dispatch/handle 无条件 future.setResult），典型受害者是闲置超时被服务端回收的死会话
	 * （LogService getLogSession==null → Procedure.LogicError）：未解码的空 Result（logs 空、
	 * remain=false）会被调用方当"查完无匹配"静默消费，SessionAll 还把该台永久标记 finished。
	 * 这里对齐 {@link #close()} 的既有检查形态：非零码在 get 时抛异常——单台 HTTP handle 的
	 * catch 返回可见 system error，SessionAll.operate 的既有 catch 走 failedServers（不标记
	 * finished，下次 operate 重试）。
	 * <p>不变式：rpc 的 resultCode 在 future.setResult 之前写入（setupRpcResponseContext 先
	 * 于 future 完成执行），故 get 返回后再读 resultCode 无竞态；RPC 自身失败（发送失败/超时）
	 * 仍由底层 future 以异常完成，原语义不变。包装仅覆盖 get/get(timeout)——全部既有调用面，
	 * 未完成的 isDone 等查询语义对本包装无意义（状态跟随底层 future）。</p>
	 */
	static TaskCompletionSource<BResult.Data> checkResultCode(Rpc<?, BResult.Data> rpc,
															  TaskCompletionSource<BResult.Data> future) {
		return new TaskCompletionSource<BResult.Data>() {
			@Override
			public BResult.Data get() {
				return checked(rpc, future.get());
			}

			@Override
			public BResult.Data get(long timeout, TimeUnit unit) {
				return checked(rpc, future.get(timeout, unit));
			}
		};
	}

	private static BResult.Data checked(Rpc<?, BResult.Data> rpc, BResult.Data result) {
		var code = rpc.getResultCode();
		// 结果码分诊：死会话（LogicError）与参数级拒绝（INVALID_ARGUMENT）各自显式抛型，
		// 其余非零码保持原 RuntimeException 形态——调用方据异常类型决策（见 isSessionLevelError）。
		if (code == Procedure.LogicError)
			throw new SessionLevelException("search/browse error " + code);
		if (code == Zeze.Services.LogService.INVALID_ARGUMENT)
			throw new InvalidArgumentException("search/browse error " + code);
		if (code != 0)
			throw new RuntimeException("search/browse error " + code);
		return result;
	}

	/**
	 * 会话级死亡（服务端拒绝本会话——典型：闲置超时被回收后的 LogicError）：调用方据此
	 * 触发驱逐重建（FileSessionManager.operateRecovering 关旧建新重试一次、SessionAll
	 * renewDeadMembers 成员自愈）；网络/参数类失败重建无益，原样上抛。
	 */
	public static final class SessionLevelException extends RuntimeException {
		public SessionLevelException(String message) {
			super(message);
		}
	}

	/**
	 * 参数级拒绝（服务端 {@code LogService.INVALID_ARGUMENT}：非法 containsType、
	 * words/pattern 双空、offsetFactor∉[0,1)）：会话仍有效，不得拆建——直接向调用方
	 * 报参数错误（ZokerManager 处理器映射为明确的 errorResult desc）。
	 */
	public static final class InvalidArgumentException extends RuntimeException {
		public InvalidArgumentException(String message) {
			super(message);
		}
	}

	/**
	 * 会话级错误判别：仅 {@link SessionLevelException}（服务端拒绝本会话）判真；网络/超时
	 * 类异常与参数级拒绝判假。调用方据此只对会话级死亡触发重建——其余失败重建无益
	 *（白白丢弃仍有效的会话与游标）或必然重蹈覆辙。
	 */
	public static boolean isSessionLevelError(Throwable e) {
		return e instanceof SessionLevelException;
	}

	private volatile boolean closed;

	@Override
	public void close() throws Exception {
		if (closed)
			return;
		closed = true; // 先立墓碑：RPC失败时会话状态未知，不允许重发CloseSession
		var r = new CloseSession();
		// 与browse/search同宽60s：服务端关会话含逐个RAF关闭，默认5s在
		// 多会话/慢盘下超时即泄漏（服务端会话无过期回收前的唯一出口）。
		r.SendForWait(readySocket(), 60_000).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("close session error " + r.getResultCode());
	}
}
