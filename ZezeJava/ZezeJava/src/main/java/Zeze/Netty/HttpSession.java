package Zeze.Netty;

import java.security.SecureRandom;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import Zeze.Application;
import Zeze.Builtin.HttpSession.BSessionValue;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Component.TimerSpec;
import Zeze.IModule;
import Zeze.Services.Token;
import Zeze.Transaction.Collections.PMap1;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.FuncLong;
import Zeze.Util.OutObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HttpSession extends AbstractHttpSession {
	public static final String ZEZE_SESSION_ID_NAME = "ZEZESESSIONID";
	public static final String GlobalHttpSessionExpiredTimer = "Zeze.Netty.HttpSession.GlobalHttpSessionExpiredTimer";

	private final @NotNull Application zeze;
	private volatile long httpSessionExpire = 15 * 60 * 1000; // default expire 15 minutes.
	private final Random tokenRandom = new SecureRandom();

	public class CookieSession {
		private final String cookieSessionId;

		public CookieSession(@NotNull String cookieSessionId) {
			this.cookieSessionId = cookieSessionId;
		}

		// FND6-14：@Get/@Post 默认 TransactionLevel.None，无事务上下文时 TableX.get 内
		// Transaction.getCurrent() 为 null（assert 运行期禁用）必 NPE。比照 getCookieSession
		// 判例：有运行事务时直接同事务访问表（行为与修复前一致），否则包短 Procedure。
		// 【注意】action 不得抛异常：无事务路径下 action 在短 Procedure 内执行，Procedure.call
		// 会吞掉异常 cause 仅返回错误码，届时调用方只能看到 "CookieSession access error=..."
		// 而丢失真实原因（df08187 残留P3）。
		private <R> R accessTable(String opName, Function<BSessionValue, R> action) {
			return accessTable(opName, action, null);
		}

		/**
		 * @param noTxResult 无事务路径（短Procedure）下对 action 结果的转换器，在短Procedure事务内执行
		 *                   （读取走事务一致视图），如 getProperties 拷贝快照；null 表示不转换。
		 *                   有事务路径不经过此转换，直接返回活引用（随事务语义）。
		 */
		private <R> R accessTable(String opName, Function<BSessionValue, R> action,
								  @Nullable UnaryOperator<R> noTxResult) {
			var t = Transaction.getCurrent();
			if (t != null && t.isRunning()) {
				var value = _tSession.get(cookieSessionId);
				if (value == null)
					throw new IllegalStateException("CookieSession not exist." + cookieSessionId);
				return action.apply(value);
			}
			var result = new OutObject<R>();
			var exists = new OutObject<>(false);
			var rc = zeze.newProcedure(() -> {
				exists.value = false; // 乐观锁 redo 整体重跑时重置 out 参数，避免沿用上一轮的陈旧结果。
				var value = _tSession.get(cookieSessionId);
				if (value != null) {
					exists.value = true;
					result.value = action.apply(value);
					if (noTxResult != null) // 在事务内完成转换（如快照拷贝），带出事务后安全。
						result.value = noTxResult.apply(result.value);
				}
				return Procedure.Success;
			}, "CookieSession." + opName).call();
			if (rc != 0L)
				throw new IllegalStateException("CookieSession access error="
						+ IModule.getErrorCode(rc) + " " + cookieSessionId);
			if (!exists.value)
				throw new IllegalStateException("CookieSession not exist." + cookieSessionId);
			return result.value;
		}

		public @Nullable String getProperty(@NotNull String key) {
			return accessTable("getProperty", value -> value.getProperties().get(key));
		}

		public void setProperty(@NotNull String key, @NotNull String value) {
			accessTable("setProperty", v -> {
				v.getProperties().put(key, value);
				return null;
			});
		}

		/**
		 * 读取全部会话属性。有事务调用返回活的 PMap1 引用，修改随当前事务提交（事务语义）。
		 * 无事务调用（如 TransactionLevel.None 的 handler）返回快照副本：活引用带出短 Procedure 后
		 * put/remove 抛 IllegalStateException（managed bean 要求事务上下文），API 不对称且报错
		 * 不指向真因（df08187 残留P3），故拷贝快照；对快照的修改不会持久化，写属性请走 setProperty。
		 */
		public @NotNull Map<String, String> getProperties() {
			return accessTable("getProperties", BSessionValue::getProperties, props -> {
				var snapshot = new PMap1<>(String.class, String.class);
				snapshot.putAll(props);
				return snapshot;
			});
		}

		public long getCreateTime() {
			return accessTable("getCreateTime", BSessionValue::getCreateTime);
		}

		public long getExpireTime() {
			return accessTable("getExpireTime", BSessionValue::getExpireTime);
		}

		public void setExpireTime(long expireTime) {
			accessTable("setExpireTime", v -> {
				v.setExpireTime(expireTime);
				return null;
			});
		}
	}

	public long getHttpSessionExpire() {
		return httpSessionExpire;
	}

	public void setHttpSessionExpire(long httpSessionExpire) {
		this.httpSessionExpire = httpSessionExpire;
	}

	private @NotNull String makeSessionId() {
		return Token.genToken(tokenRandom);
	}

	public @NotNull CookieSession getCookieSession(@NotNull HttpExchange x) throws Exception {
		// 这个不缓存了，也不共享，http请求结束就可以释放。
		var cookieSessionId = x.getCookie(ZEZE_SESSION_ID_NAME);
		if (cookieSessionId == null) {
			cookieSessionId = makeSessionId();
		}
		final var finalId = cookieSessionId;
		final var needSetCookie = new OutObject<>(false);
		FuncLong initAction = () -> {
			needSetCookie.value = false; // 乐观锁 redo 整体重跑时重置 out 参数，避免沿用上一轮的陈旧结果。
			var isAdd = new OutObject<>(false);
			var value = _tSession.getOrAdd(finalId, isAdd);
			var now = System.currentTimeMillis();
			var expire = httpSessionExpire;
			if (isAdd.value || value.getExpireTime() <= now) {
				// 初始化 HttpSession
				value.setCreateTime(now);
				value.setExpireTime(now + expire);
				value.getProperties().clear();
				needSetCookie.value = true;
			}
			return Procedure.Success;
		};
		// 本方法不再由channelRead在EventLoop线程同步调用(DB事务阻塞IO线程,DB抖动期间该EventLoop上
		// 所有连接的读写全部停摆),而是延迟到用户handler内首次调用HttpExchange.getCookieSession()时执行:
		// 此时通常已运行在派发到池的用户事务里,直接同事务访问表(连独立事务的开销都省了);
		// 无事务上下文时(Level=None的处理器等)在调用线程包一个短Procedure。
		var t = Transaction.getCurrent();
		long rc;
		if (t != null && t.isRunning()) {
			initAction.call();
			rc = Procedure.Success;
		} else
			rc = zeze.newProcedure(initAction, "initCookieSession").call();
		if (rc != 0L)
			throw new RuntimeException("initCookieSession error=" + IModule.getErrorCode(rc));
		if (needSetCookie.value)
			x.setCookie(ZEZE_SESSION_ID_NAME, finalId, null, null, httpSessionExpire / 1000);
		return new CookieSession(finalId); // value 不能记住，每次访问重新从表中读取。
	}

	public HttpSession(@NotNull Application zeze) {
		this.zeze = zeze;
		zeze.getAppBase().addModule(this);
	}

	public void start() throws ParseException {
		// 全局一个timer实例，会忽略重复注册调用。
		// 不取消。
		// scheduleNamed 是表操作，需要在事务内执行；HttpServer.start 调用时没有事务，这里包一个短Procedure。
		var rc = zeze.newProcedure(() -> {
			zeze.getTimer().scheduleNamed(GlobalHttpSessionExpiredTimer,
					TimerSpec.ofCron("0 0 5 * * ?"),
					ExpiredTimer.class);
			return Procedure.Success;
		}, "enableHttpSessionExpiredTimer").call();
		if (rc != 0L)
			throw new RuntimeException("enableHttpSessionExpiredTimer error=" + IModule.getErrorCode(rc));
	}

	public void stop() {
		zeze.getAppBase().removeModule(this);
	}

	@NotNull Zeze.Builtin.HttpSession.tSession tSession() {
		return _tSession;
	}

	public static class ExpiredTimer implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) throws Exception {
			var httpSession = (HttpSession)context.timer.zeze.getAppBase().getModules().get(HttpSession.ModuleFullName);
			if (null != httpSession) {
				var now = System.currentTimeMillis();
				var batch = new RemoveBatch(httpSession.tSession());
				httpSession.tSession().walk((key, value) -> {
					if (value.getExpireTime() <= now)
						batch.add(key);
					return true;
				});
				batch.tryPerform();
			}
		}
	}

	private static class RemoveBatch {
		private final @NotNull Zeze.Builtin.HttpSession.tSession tSession;
		private final ArrayList<String> keys = new ArrayList<>();

		public RemoveBatch(@NotNull Zeze.Builtin.HttpSession.tSession tSession) {
			this.tSession = tSession;
		}

		public void add(@NotNull String key) {
			keys.add(key);
			if (keys.size() >= 10)
				tryPerform();
		}

		private void tryPerform() {
			if (!keys.isEmpty()) {
				tSession.getZeze().newProcedure(() -> {
					for (var key : keys)
						tSession.remove(key);
					return 0;
				}, "remove http session").call();
				keys.clear();
			}
		}
	}
}
