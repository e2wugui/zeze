package Zeze.Game;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Game.Online.Logout;
import Zeze.Builtin.Game.Online.BLink;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Util.EventDispatcher;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-75回归：三个协议顶层入口吞掉内层失败码、恒返Success，半程写入随外层事务提交——
 * Game.ProviderWithOnline.ProcessLinkBroken丢弃onlineSet.linkBroken的rc（linkBroken/
 * LocalRemove事件链半触发、DelayLogout缺失）、Game.Online.ProcessLogoutRequestOnlineSet
 * 丢弃localLogout的rc（logout事件链半触发，孪生T1）、Arch.ProviderWithOnline.
 * ProcessLinkBroken丢弃online.linkBroken的rc（stale-local分支removeLocalAndTrigger
 * 半触发）。三者与Online.linkBroken的FND5-41"失败码外传整体回滚"契约及其余五入口
 * （triggerLinkBroken/sendDirect/redirectRemoveLocal/processOffline/DelayLogout）
 * 相反。修复后：rc透传，失败→Procedure.call回滚，状态机不推进；孪生T1的respond
 * 移到判定之后，失败不再发出成功应答。
 */
@Fast
public class TestFnd875RcPassthrough {

	// a6专属serverId段（上限16383内，避开默认0/100/300/7250/7350/7360等既有段）。
	private static final AtomicInteger NextServerId = new AtomicInteger(16191);

	private static Application newApp(String name) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("a6_fnd875_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application(name, conf);
	}

	// 走initialize（linkBroken/localLogout路径的DelayLogout调度需要App自有Timer）。
	// ProcessLinkBroken按onlineSetName经providerImplement定位Online（fake ProviderApp下
	// providerImplement为null），以反射布线（对齐TestFnd872OfflineTimerBookkeeping手法）。
	private static Online newOnline(Application zeze) throws ReflectiveOperationException {
		var app = new AppBase() {
			@Override
			public Application getZeze() {
				return zeze;
			}
		};
		var providerApp = new ProviderApp(zeze);
		zeze.initialize(app);
		var online = new Online(app);
		var pwo = new ProviderWithOnline() {
		};
		var onlineField = ProviderWithOnline.class.getDeclaredField("online");
		onlineField.setAccessible(true);
		onlineField.set(pwo, online);
		var implField = ProviderApp.class.getDeclaredField("providerImplement");
		implField.setAccessible(true);
		implField.set(providerApp, pwo);
		return online;
	}

	private static final class TestArchOnline extends Zeze.Arch.Online {
		TestArchOnline(@NotNull AppBase app) {
			super(app);
		}

		Zeze.Builtin.Online.tlocal tlocal() {
			return _tlocal; // protected跨包仅子类内可达
		}
	}

	// protected入口以子类放开（跨包继承，方法与字段在子类内可达）。
	private static final class ArchProvider extends Zeze.Arch.ProviderWithOnline {
		ArchProvider(@NotNull Zeze.Arch.Online online) {
			this.online = online;
		}

		long call(@NotNull LinkBroken p) throws Exception {
			return ProcessLinkBroken(p);
		}
	}

	// 最小哑socket：仅为handler日志与协议发送提供载体（Send恒false）。
	// getLinkName(sender)=sender.getConnector().getName()，覆写提供稳定link名。
	private static final class StubSocket extends AsyncSocket {
		StubSocket() {
			super(null);
		}

		@Override
		public @Nullable Connector getConnector() {
			return new Connector("127.0.0.1", 1) {
				@Override
				public @NotNull String getName() {
					return "link1";
				}
			};
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

	private static LinkBroken newLinkBroken(String account, String context) {
		var p = new LinkBroken();
		p.Argument.setAccount(account);
		p.Argument.setLinkSid(42L); // 与前置伪造的BLink.linkSid一致，通过归属校验
		p.Argument.getUserState().setContext(context);
		p.setSender(new StubSocket());
		return p;
	}

	// Game侧前置登录态：shared与local版本一致、eLogined（进入linkBrokenTrigger/logoutTrigger
	// 主路径而非stale-local分支）。仿TestGameLinkBrokenTriggerRc的setup。
	private static void forgeGameLogin(Online online, long roleId) {
		Assertions.assertEquals(0, online.providerApp.zeze.newProcedure(() -> {
			var shared = online.getOrAddOnlineShared(roleId);
			shared.setAccount("a6acc");
			shared.setLoginVersion(1L); // logoutVersion(0)不等，localLogout的assignLogoutVersion才推进
			shared.setLink(new BLink("link1", 42L, AbstractOnline.eLogined));
			// _tOnline行标记本机：logoutTrigger的tryRedirectRemoveLocal对非本机serverId
			// 会触碰fake ProviderApp的null providerDirectService（本机则短路早退）。
			online.getOrAddOnline(roleId).setServerId(online.providerApp.zeze.getConfig().getServerId());
			var local = online._tlocal.getOrAdd(roleId);
			local.setLink(new BLink("link1", 42L, AbstractOnline.eLogined));
			local.setLoginVersion(shared.getLoginVersion());
			return 0;
		}, "a6.fnd875.setup").call());
	}

	private static int readState(Online online, long roleId) {
		var state = new int[1];
		Assertions.assertEquals(0, online.providerApp.zeze.newProcedure(() -> {
			var shared = online.getOnlineShared(roleId);
			state[0] = shared != null ? shared.getLink().getState() : AbstractOnline.eOffline;
			return 0;
		}, "a6.fnd875.verify").call());
		return state[0];
	}

	/** 主：Game.ProcessLinkBroken透传onlineSet.linkBroken的失败码，状态机不随半程写入推进。 */
	@Test
	public void testGameLinkBrokenRcPassedThrough() throws Exception {
		var zeze = newApp("TestFnd875GameLb");
		var online = newOnline(zeze);
		zeze.start();
		try {
			final var errRc = -234561;
			online.getLinkBrokenEvents().add(EventDispatcher.Mode.RunEmbed, (__s, __a) -> errRc);
			var roleId = 161_901L;
			forgeGameLogin(online, roleId);

			var provider = new ProviderWithOnline();
			provider.online = online; // protected字段，同包注入
			var p = newLinkBroken("a6acc", Long.toString(roleId));
			// 真实派发形态：handler在Serializable事务内执行，失败码决定整体回滚。
			var rc = zeze.newProcedure(() -> provider.ProcessLinkBroken(p), "a6.fnd875.gameLb").call();
			Assertions.assertEquals(errRc, rc, "断线处理器错误码必须经入口外传（修复前恒Success）");
			Assertions.assertEquals(AbstractOnline.eLogined, readState(online, roleId),
					"失败处理器的半程写入不得随Success提交（eLinkBroken推进须回滚）");
		} finally {
			try {
				zeze.stop();
			} catch (Exception ignored) {
			}
		}
	}

	/** 孪生T1：Game.ProcessLogoutRequestOnlineSet透传localLogout失败码，且失败不respond。 */
	@Test
	public void testGameLogoutRequestRcPassedThrough() throws Exception {
		var zeze = newApp("TestFnd875GameLogout");
		var online = newOnline(zeze);
		zeze.start();
		try {
			final var errRc = -234562;
			online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (__s, __a) -> errRc);
			var roleId = 161_902L;
			forgeGameLogin(online, roleId);

			final var respondCalls = new AtomicInteger();
			// Dispatch仅为类型占位，覆写后不触达；respond覆写为计数（失败路径不得调用）。
			var session = new ProviderUserSession(null) {
				@Override
				public @Nullable Long getRoleId() {
					return roleId;
				}

				@Override
				public long getLinkSid() {
					return 42L;
				}

				@Override
				public @NotNull String getOnlineSetName() {
					return "";
				}

				@Override
				public void respond(@NotNull Protocol<?> p) {
					respondCalls.incrementAndGet();
				}
			};
			var rpc = new Logout();
			rpc.setSender(new StubSocket());
			rpc.setUserState(session);

			var rc = zeze.newProcedure(() -> online.ProcessLogoutRequestOnlineSet(rpc), "a6.fnd875.gameLogout").call();
			Assertions.assertEquals(errRc, rc, "logout事件链失败码必须经入口外传（修复前恒Success）");
			Assertions.assertEquals(0, respondCalls.get(), "失败回滚时不得发出成功应答（respond已移到判定之后）");
			Assertions.assertEquals(AbstractOnline.eLogined, readState(online, roleId),
					"半程登出写入不得随Success提交");
		} finally {
			try {
				zeze.stop();
			} catch (Exception ignored) {
			}
		}
	}

	/** Arch版：stale-local分支removeLocalAndTrigger失败码经ProcessLinkBroken外传。 */
	@Test
	public void testArchLinkBrokenRcPassedThrough() throws Exception {
		var zeze = newApp("TestFnd875ArchLb");
		var app = new AppBase() {
			@Override
			public Application getZeze() {
				return zeze;
			}
		};
		new ProviderApp(zeze);
		zeze.initialize(app);
		var archOnline = new TestArchOnline(app); // 表注册纯内存，须在start前构造
		zeze.start();
		try {
			final var errRc = -234563;
			archOnline.getLocalRemoveEvents().add(EventDispatcher.Mode.RunEmbed, (__s, __a) -> errRc);
			// 前置：tonline侧login（link/版本2/本机）与tlocal侧版本1不一致 → linkBroken走
			// removeLocalAndTrigger（stale-local分支），其embed失败即本用例注入点。
			Assertions.assertEquals(0, zeze.newProcedure(() -> {
				var login = archOnline.getOrAddOnline("a6acc").getLogins().getOrAdd("a6cid");
				login.setLink(new Zeze.Builtin.Online.BLink("link1", 42L, Zeze.Arch.AbstractOnline.eLogined));
				login.setLoginVersion(2L);
				login.setServerId(zeze.getConfig().getServerId());
				archOnline.tlocal().getOrAdd("a6acc").getLogins().getOrAdd("a6cid").setLoginVersion(1L);
				return 0;
			}, "a6.fnd875.archSetup").call());

			var provider = new ArchProvider(archOnline);
			var p = newLinkBroken("a6acc", "a6cid");
			var rc = zeze.newProcedure(() -> provider.call(p), "a6.fnd875.archLb").call();
			Assertions.assertEquals(errRc, rc, "Arch版断线失败码必须经入口外传（修复前恒Success）");
		} finally {
			try {
				zeze.stop();
			} catch (Exception ignored) {
			}
		}
	}
}
