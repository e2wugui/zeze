package Zeze.Netty;

import java.security.SecureRandom;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import Zeze.Application;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Services.Token;
import Zeze.Util.OutObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HttpSession extends AbstractHttpSession {
	public static final String ZEZE_SESSION_ID_NAME = "ZEZESESSIONID";

	public class CookieSession {
		private final String cookieSessionId;

		public CookieSession(@NotNull String cookieSessionId) {
			this.cookieSessionId = cookieSessionId;
		}

		public @Nullable String getProperty(String key) {
			var value = _tSession.get(cookieSessionId);
			if (null != value)
				return value.getProperties().get(key);
			throw new IllegalStateException("CookieSession not exist." + cookieSessionId);
		}

		public void setProperty(@NotNull String key, @NotNull String value) {
			var tValue = _tSession.get(cookieSessionId);
			if (null != tValue)
				tValue.getProperties().put(key, value);
			throw new IllegalStateException("CookieSession not exist." + cookieSessionId);
		}

		public Map<String, String> getProperties() {
			var tValue = _tSession.get(cookieSessionId);
			if (null != tValue)
				return tValue.getProperties();
			throw new IllegalStateException("CookieSession not exist." + cookieSessionId);
		}

		public long getCreateTime() {
			var value = _tSession.get(cookieSessionId);
			if (null != value)
				return value.getCreateTime();
			throw new IllegalStateException("CookieSession not exist." + cookieSessionId);
		}

		public long getExpireTime() {
			var value = _tSession.get(cookieSessionId);
			if (null != value)
				return value.getExpireTime();
			throw new IllegalStateException("CookieSession not exist." + cookieSessionId);
		}

		public void setExpireTime(long expireTime) {
			var value = _tSession.get(cookieSessionId);
			if (null != value)
				value.setExpireTime(expireTime);
			throw new IllegalStateException("CookieSession not exist." + cookieSessionId);
		}
	}

	private volatile long httpSessionExpire = 15 * 60 * 1000; // default expire 15 minutes.
	private final Random tokenRandom = new SecureRandom();

	public long getHttpSessionExpire() {
		return httpSessionExpire;
	}

	public void setHttpSessionExpire(long httpSessionExpire) {
		this.httpSessionExpire = httpSessionExpire;
	}

	private @NotNull String makeSessionid() {
		return Token.genToken(tokenRandom);
	}

	public @NotNull CookieSession getCookieSession(@NotNull HttpExchange x) {
		// 这个不缓存了，也不共享，http请求结束就可以释放。
		var isAdd = new OutObject<>(false);
		var cookieSessionId = x.getCookie(ZEZE_SESSION_ID_NAME);
		if (cookieSessionId == null) {
			cookieSessionId = makeSessionid();
			isAdd.value = true;
		}
		var value = _tSession.getOrAdd(cookieSessionId, isAdd);
		var now = System.currentTimeMillis();
		var expire = httpSessionExpire;
		if (isAdd.value || value.getExpireTime() <= now) {
			// 初始化 HttpSession
			value.setCreateTime(now);
			value.setExpireTime(now + expire);
			value.getProperties().clear();
			x.setCookie(ZEZE_SESSION_ID_NAME, cookieSessionId, null, null, expire / 1000);
		}
		return new CookieSession(cookieSessionId); // value 不能记住，每次访问重新从表中读取。
	}

	public static final String GlobalHttpSessionExpiredTimer = "Zeze.Netty.HttpSession.GlobalHttpSessionExpiredTimer";

	private final Application zeze;

	public HttpSession(Application zeze) {
		this.zeze = zeze;
		zeze.getAppBase().addModule(this);
	}

	public void start() throws ParseException {
		// 全局一个timer实例，会忽略重复注册调用。
		// 不取消。
		zeze.getTimer().scheduleNamed(GlobalHttpSessionExpiredTimer, "0 0 5 * * ?", ExpiredTimer.class, null);
	}

	public void stop() {
		zeze.getAppBase().removeModule(this);
	}

	Zeze.Builtin.HttpSession.tSession tSession() {
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

		@Override
		public void onTimerCancel() throws Exception {

		}
	}

	private static class RemoveBatch {
		private final Zeze.Builtin.HttpSession.tSession tSession;
		private final ArrayList<String> keys = new ArrayList<>();

		public RemoveBatch(Zeze.Builtin.HttpSession.tSession tSession) {
			this.tSession = tSession;
		}

		public void add(String key) {
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
				}, "remove http session");
				keys.clear();
			}
		}
	}
}
