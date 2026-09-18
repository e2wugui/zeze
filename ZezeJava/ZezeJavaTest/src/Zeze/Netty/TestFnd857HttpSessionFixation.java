package Zeze.Netty;

import harness.Fast;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-57回归：HttpSession会话固定——客户端提供的ZEZESESSIONID不论是否已知都被原样作主键
 * 建行（getOrAdd），新会话的id就是客户端提供的值；攻击者经cookie注入把自选id植入受害者，
 * 受害者首访即以该id建全新会话，攻击者持同id并发访问即获得其会话；过期id同样以旧主键复活。
 * 修复后：新建会话的id永远由服务器随机生成——客户端id只在表中已存在且未过期时才被采用，
 * 未知/过期一律丢弃再生；Set-Cookie补HttpOnly/SameSite=Lax硬化。
 */
@Fast
public class TestFnd857HttpSessionFixation {
	// a4专属serverId段（1410起，serverId上界16383）：本地缓存目录/dbhome按serverId分目录，
	// 避开默认0与他组撞车
	private static final AtomicInteger NextServerId = new AtomicInteger(1410);

	private static final class TestAppBase extends AppBase {
		private final Application zeze;

		TestAppBase(Application zeze) {
			this.zeze = zeze;
		}

		@Override
		public Application getZeze() {
			return zeze;
		}
	}

	/** 独立编程式Application（SM=disable、Memory库独立桶）+已注册会话表。 */
	private static final class TestEnv implements AutoCloseable {
		final Application app;
		final HttpSession httpSession;

		TestEnv() throws Exception {
			Task.tryInitThreadPool();
			var conf = new Config();
			conf.setServiceManager("disable");
			int serverId = NextServerId.getAndIncrement();
			conf.setServerId(serverId);
			conf.setDefaultTableConf(new Config.TableConf());
			var dbConf = new Config.DatabaseConf();
			dbConf.setDatabaseUrl("a4_fnd857_" + serverId);
			conf.getDatabaseConfMap().putIfAbsent("", dbConf);
			app = new Application("a4.fnd857." + serverId, conf);
			app.initialize(new TestAppBase(app));
			httpSession = new HttpSession(app);
			httpSession.RegisterZezeTables(app);
			app.start();
		}

		@Override
		public void close() throws Exception {
			app.stop();
		}

		@Nullable Zeze.Builtin.HttpSession.BSessionValue getRow(String id) {
			var row = new OutObject<Zeze.Builtin.HttpSession.BSessionValue>();
			var rc = app.newProcedure(() -> {
				row.value = httpSession.tSession().get(id);
				return Procedure.Success;
			}, "a4.fnd857.get").call();
			Assertions.assertEquals(0L, rc);
			return row.value;
		}
	}

	/** 直填request携带Cookie的HttpExchange（无服务器构造，仅供getCookieSession读写）。 */
	private static final class TestExchange extends HttpExchange {
		TestExchange(@Nullable String cookie) {
			super(new HttpServer(), null);
			var req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/a4_fnd857");
			if (cookie != null)
				req.headers().set(HttpHeaderNames.COOKIE, cookie);
			request = req;
		}

		@Nullable String setCookieHeader() {
			if (resHeaders == null)
				return null;
			for (int i = 0; i < resHeaders.size(); i += 2)
				if (HttpHeaderNames.SET_COOKIE.contentEquals((CharSequence)resHeaders.get(i)))
					return String.valueOf(resHeaders.get(i + 1));
			return null;
		}
	}

	// 未知（客户端自选）id：不得以该id建行——id再生为服务器随机值，Set-Cookie回写新id并带硬化属性
	@Test
	public void testUnknownClientIdRegenerated() throws Exception {
		try (var env = new TestEnv()) {
			var attackerId = "a4-attacker-fixed-id";
			var x = new TestExchange(HttpSession.ZEZE_SESSION_ID_NAME + "=" + attackerId);
			var cs = env.httpSession.getCookieSession(x);
			cs.setProperty("who", "victim");

			Assertions.assertNull(env.getRow(attackerId),
					"客户端提供的未知id不得成为会话行主键（session fixation，修复前以该id建行）");
			var setCookie = x.setCookieHeader();
			Assertions.assertNotNull(setCookie, "新建会话必须Set-Cookie");
			var newId = setCookie.substring((HttpSession.ZEZE_SESSION_ID_NAME + "=").length()).split(";", 2)[0];
			Assertions.assertNotEquals(attackerId, newId, "新建会话的id必须是服务器再生值");
			Assertions.assertNotNull(env.getRow(newId), "再生id建行");
			Assertions.assertEquals("victim", cs.getProperty("who"), "属性写入再生id的行");
			Assertions.assertTrue(setCookie.toLowerCase().contains("httponly"), "会话cookie必须HttpOnly: " + setCookie);
			Assertions.assertTrue(setCookie.contains("SameSite=Lax"), "会话cookie必须SameSite=Lax: " + setCookie);
		}
	}

	// 合法续期零影响：已存在且未过期的id照常沿用，不Set-Cookie、不换id
	@Test
	public void testValidContinuationKeepsId() throws Exception {
		try (var env = new TestEnv()) {
			var x1 = new TestExchange(null); // 无cookie：服务器生成
			env.httpSession.getCookieSession(x1).setProperty("n", "1");
			var setCookie1 = x1.setCookieHeader();
			Assertions.assertNotNull(setCookie1, "首访必须Set-Cookie");
			var id = setCookie1.substring((HttpSession.ZEZE_SESSION_ID_NAME + "=").length()).split(";", 2)[0];
			Assertions.assertFalse(id.isEmpty());
			Assertions.assertNotNull(env.getRow(id), "服务器生成id建行");

			var x2 = new TestExchange(HttpSession.ZEZE_SESSION_ID_NAME + "=" + id);
			var cs2 = env.httpSession.getCookieSession(x2);
			Assertions.assertEquals("1", cs2.getProperty("n"), "合法id续会话，属性延续");
			Assertions.assertNull(x2.setCookieHeader(), "未过期续期不应重复Set-Cookie");
		}
	}

	// 已过期的id：不得以旧主键复活——同样再生新id建新行，旧行保持过期原样
	@Test
	public void testExpiredIdNotRevived() throws Exception {
		try (var env = new TestEnv()) {
			var expiredId = "a4-expired-id";
			var rc = env.app.newProcedure(() -> {
				var row = env.httpSession.tSession().getOrAdd(expiredId);
				row.setCreateTime(1);
				row.setExpireTime(2); // 远在过去
				row.getProperties().clear();
				return Procedure.Success;
			}, "a4.fnd857.seed").call();
			Assertions.assertEquals(0L, rc);

			var x = new TestExchange(HttpSession.ZEZE_SESSION_ID_NAME + "=" + expiredId);
			var cs = env.httpSession.getCookieSession(x);
			cs.setProperty("who", "new");

			var old = env.getRow(expiredId);
			Assertions.assertNotNull(old);
			Assertions.assertEquals(2L, old.getExpireTime(), "过期id不得就地复活（修复前旧主键被重置续期）");

			var setCookie = x.setCookieHeader();
			Assertions.assertNotNull(setCookie);
			var newId = setCookie.substring((HttpSession.ZEZE_SESSION_ID_NAME + "=").length()).split(";", 2)[0];
			Assertions.assertNotEquals(expiredId, newId, "Set-Cookie不得回写过期id: " + setCookie);
			Assertions.assertNotNull(env.getRow(newId), "再生id建行");
			Assertions.assertEquals("new", cs.getProperty("who"), "新会话属性写入再生id的行");
		}
	}
}
