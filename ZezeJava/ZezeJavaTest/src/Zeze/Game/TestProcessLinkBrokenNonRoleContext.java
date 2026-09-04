package Zeze.Game;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Transaction.Procedure;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND2-G1-4 回归：BUserState.context 是登录身份槽——Game角色模式存roleId数字串
 * (Game/Online.java:2096/2195)，混合装配下Arch账号模式登录写入clientId任意字符串
 * (Arch/Online.java:1783/1859)。ProcessLinkBroken 裸 Long.parseLong(context)：
 * 账号会话断链即抛 NumberFormatException 逃出handler，linkBroken被跳过（无
 * eLinkBroken状态、无延迟登出定时器），幽灵在线残留到verifyLocal（默认600s）。
 * 约定（镜像50e9520d5的getRoleId）：解析失败=非角色会话，返回null跳过，不抛。
 * 自包含（不依赖外部进程与数据库，不启动网络），标@Fast。
 * 本测试与被测类同包（Zeze.Game），可直接调用protected ProcessLinkBroken并注入
 * protected online字段（harness同TestOnlineHotStopEventRef）。
 */
@Fast
public class TestProcessLinkBrokenNonRoleContext {

	// 与其他 @Fast 测试错开 serverId：并行时 Application 本地缓存按 serverId 一份。
	private static final AtomicInteger NextServerId = new AtomicInteger(7250);

	private static Application newApp(String name) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("link_broken_parse_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application(name, conf);
	}

	// 本测试不访问表数据，不走 start()：fake ProviderApp 仅建立 zeze.redirect，
	// Online 构造里的 RegisterProtocols/RegisterZezeTables 都是纯内存注册。
	// providerImplement 保持null：非角色会话用例在linkBroken之前即跳过，不会触达。
	private static Online newOnline(Application zeze) {
		var app = new AppBase() {
			@Override
			public Application getZeze() {
				return zeze;
			}
		};
		new ProviderApp(zeze);
		return new Online(app);
	}

	// 最小哑socket：仅为handler日志分支（ENABLE_PROTOCOL_LOG=false时）提供getSessionId()。
	private static final class StubSocket extends AsyncSocket {
		StubSocket() {
			super(null);
		}

		@Override
		public Type getType() {
			return null;
		}

		@Override
		public boolean close(@Nullable Throwable ex, boolean gracefully) {
			return false;
		}

		@Override
		public boolean Send(byte @NotNull[] bytes, int offset, int length) {
			return false;
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}

		@Override
		public boolean isClosed() {
			return true;
		}
	}

	private static LinkBroken newLinkBroken(String context) {
		var p = new LinkBroken();
		p.Argument.getUserState().setContext(context);
		p.setSender(new StubSocket());
		return p;
	}

	@Test
	public void testNonNumericContextSkipsWithoutThrow() throws Exception {
		// 修复前：online!=null进入Long.parseLong("device-abc-123")抛NumberFormatException
		// 逃出handler，断链处理被跳过且每次触发一条异常日志。
		var provider = new ProviderWithOnline();
		provider.online = newOnline(newApp("TestLinkBrokenNonRole1"));
		var p = newLinkBroken("device-abc-123");
		Assertions.assertDoesNotThrow(() -> {
			Assertions.assertEquals(Procedure.Success, provider.ProcessLinkBroken(p));
		});
	}

	@Test
	public void testEmptyAndNumericContextWithoutOnline() throws Exception {
		// online==null（未create）时：空context（未登录）与数字roleId（角色模式）
		// 走既有路径正常返回Success，不抛（既有行为不变）。
		var provider = new ProviderWithOnline();
		Assertions.assertEquals(Procedure.Success, provider.ProcessLinkBroken(newLinkBroken("")));
		Assertions.assertEquals(Procedure.Success, provider.ProcessLinkBroken(newLinkBroken("123456")));
	}

	@Test
	public void testParseRoleIdVariants() {
		// 账号在线模式：clientId非数字串 → null（非角色会话，没有roleId）
		Assertions.assertNull(ProviderWithOnline.parseRoleId("device-abc-123"));
		// 未登录：空串 → null（既有行为不变）
		Assertions.assertNull(ProviderWithOnline.parseRoleId(""));
		// Game角色模式：数字roleId照常解析（既有行为不变）
		Assertions.assertEquals(Long.valueOf(123456), ProviderWithOnline.parseRoleId("123456"));
	}
}
