package Zeze.Netty;

import java.util.Objects;
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
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import org.jspecify.annotations.Nullable;

/**
 * 基于 cookie 的 HTTP 会话：会话表（BSession）存取、会话 id 生成与过期清理定时器。
 */
public class HttpSession extends AbstractHttpSession {
	public static final String ZEZE_SESSION_ID_NAME = "ZEZESESSIONID";
	public static final String GlobalHttpSessionExpiredTimer = "Zeze.Netty.HttpSession.GlobalHttpSessionExpiredTimer";

	private final Application zeze;
	private volatile long httpSessionExpire = 15 * 60 * 1000; // default expire 15 minutes.
	private final Random tokenRandom = new SecureRandom();

	public class CookieSession {
		private final String cookieSessionId;

		public CookieSession(String cookieSessionId) {
			this.cookieSessionId = cookieSessionId;
		}

		boolean existsInCurrentTransaction() {
			return _tSession.get(cookieSessionId) != null;
		}

		// 有运行事务时直接同事务访问表，否则包短 Procedure；action 异常经 actionError 透传。
		private <R extends @Nullable Object> R accessTable(String opName, Function<BSessionValue, R> action) {
			return accessTable(opName, action, null);
		}

		/**
		 * @param noTxResult 无事务路径下在短 Procedure 内对结果的转换器（如拷贝快照），null 表示不转换。
		 */
		private <R extends @Nullable Object> R accessTable(String opName, Function<BSessionValue, R> action,
								  @Nullable UnaryOperator<R> noTxResult) {
			var t = Transaction.getCurrent();
			if (t != null && t.isRunning()) {
				var value = _tSession.get(cookieSessionId);
				if (value == null)
					throw new IllegalStateException("CookieSession not exist." + cookieSessionId);
				return action.apply(value);
			}
			var result = new OutObject<R>();
			var actionError = new OutObject<RuntimeException>();
			var exists = new OutObject<>(false);
			var rc = zeze.newProcedure(() -> {
				exists.value = false; // redo 重跑时重置 out 参数
				actionError.value = null;
				var value = _tSession.get(cookieSessionId);
				if (value != null) {
					exists.value = true;
					try {
						R computed = action.apply(value);
						result.value = computed;
						if (noTxResult != null)
							result.value = noTxResult.apply(computed);
					} catch (RuntimeException e) {
						// 记录真实异常后再重抛以触发回滚，避免被 Procedure.call 错误码吞掉。
						actionError.value = e;
						throw e;
					}
				}
				return Procedure.Success;
			}, "CookieSession." + opName).call();
			if (rc != 0L)
				throw new IllegalStateException("CookieSession access error="
						+ IModule.getErrorCode(rc) + " " + cookieSessionId, actionError.value);
			//noinspection DataFlowIssue
			if (!exists.value)
				throw new IllegalStateException("CookieSession not exist." + cookieSessionId);
			return result.value;
		}

		public @Nullable String getProperty(String key) {
			return accessTable("getProperty", value -> value.getProperties().get(key));
		}

		public void setProperty(String key, String value) {
			accessTable("setProperty", v -> {
				v.getProperties().put(key, value);
				return null;
			});
		}

		/**
		 * 读取全部会话属性：有事务时返回受管 PMap1 引用；无事务时返回快照副本（写属性请走 {@link #setProperty}）。
		 */
		public Map<String, String> getProperties() {
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

	private String makeSessionId() {
		return Token.genToken(tokenRandom);
	}

	public CookieSession getCookieSession(HttpExchange x) throws Exception {
		// 不缓存也不共享，请求结束即可释放。
		var cookieSessionId = x.getCookie(ZEZE_SESSION_ID_NAME);
		final var needSetCookie = new OutObject<>(false);
		final var sessionId = new OutObject<String>();
		FuncLong initAction = () -> {
			needSetCookie.value = false; // redo 重跑时重置 out 参数
			var existing = cookieSessionId != null ? _tSession.get(cookieSessionId) : null;
			// 读取可能等待锁或后端，按读取完成后的时间判断过期。
			var now = System.currentTimeMillis();
			if (existing != null && existing.getExpireTime() > now) {
				sessionId.value = cookieSessionId;
				return Procedure.Success;
			}
			// 新会话只使用服务器生成的未占用 ID，冲突时重试。
			var value = new BSessionValue();
			value.setCreateTime(now);
			value.setExpireTime(now + httpSessionExpire);
			String id;
			do {
				id = makeSessionId();
			} while (id.equals(cookieSessionId) || !_tSession.tryAdd(id, value));
			sessionId.value = id;
			needSetCookie.value = true;
			return Procedure.Success;
		};
		// 延迟到业务线程首次访问时执行，避免在 EventLoop 阻塞 DB 事务。
		var t = Transaction.getCurrent();
		long rc;
		if (t != null && t.isRunning()) {
			initAction.call();
			rc = Procedure.Success;
		} else
			rc = zeze.newProcedure(initAction, "initCookieSession").call();
		if (rc != 0L)
			throw new RuntimeException("initCookieSession error=" + IModule.getErrorCode(rc));
		//noinspection DataFlowIssue
		if (needSetCookie.value) {
			// Netty Cookie 接口无 SameSite 属性，编码后手动追加。
			var cookie = new DefaultCookie(ZEZE_SESSION_ID_NAME, sessionId.value);
			cookie.setHttpOnly(true);
			cookie.setMaxAge(httpSessionExpire / 1000);
			x.setSessionCookieHeader(ServerCookieEncoder.LAX.encode(cookie) + "; SameSite=Lax");
		}
		return new CookieSession(Objects.requireNonNull(sessionId.value)); // value 不能记住，每次访问重新从表中读取。
	}

	public HttpSession(Application zeze) {
		this.zeze = zeze;
		//noinspection DataFlowIssue
		zeze.getAppBase().addModule(this);
	}

	public void start() throws ParseException {
		// 幂等补登模块，支持 stop 后重启时 ExpiredTimer 仍能查到本模块。
		//noinspection DataFlowIssue
		zeze.getAppBase().addModule(this);
		// 全局唯一命名定时器（忽略重复注册），scheduleNamed 为表操作需在短 Procedure 内执行。
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
		//noinspection DataFlowIssue
		zeze.getAppBase().removeModule(this);
	}

	Zeze.Builtin.HttpSession.tSession tSession() {
		return _tSession;
	}

	public static class ExpiredTimer implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) throws Exception {
			// AppBase.addModule 按短名 ModuleName 登记。
			//noinspection DataFlowIssue
			var httpSession = (HttpSession)context.timer.zeze.getAppBase().getModules().get(HttpSession.ModuleName);
			if (null != httpSession) {
				var now = System.currentTimeMillis();
				var batch = new RemoveBatch(httpSession.tSession(), now);
				httpSession.tSession().walk((key, value) -> {
					if (value.getExpireTime() <= now)
						batch.add(key);
					return true;
				});
				batch.tryPerform();
			}
		}
	}

	static class RemoveBatch {
		private final Zeze.Builtin.HttpSession.tSession tSession;
		private final long cutoff;
		private final ArrayList<String> keys = new ArrayList<>();

		public RemoveBatch(Zeze.Builtin.HttpSession.tSession tSession, long cutoff) {
			this.tSession = tSession;
			this.cutoff = cutoff;
		}

		public void add(String key) {
			keys.add(key);
			if (keys.size() >= 10)
				tryPerform();
		}

		void tryPerform() {
			if (!keys.isEmpty()) {
				tSession.getZeze().newProcedure(() -> {
					for (var key : keys) {
						// 事务内重验过期时间，避免误删扫描后刚被续期的会话。
						var session = tSession.get(key);
						if (session != null && session.getExpireTime() <= cutoff)
							tSession.remove(key);
					}
					return 0;
				}, "remove http session").call();
				keys.clear();
			}
		}
	}
}
