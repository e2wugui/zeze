package Zezex;
import org.junit.jupiter.api.Test;

import Game.Fight.IModuleFight;
import Zeze.Builtin.Game.Online.ReLogin;
import Zeze.Util.Task;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.Assertions;

public class TestOnline {
	static {
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(Level.INFO);
	}

	private static final Logger logger = LogManager.getLogger(TestOnline.class);

	final ZezexTestEnv env = new ZezexTestEnv();

	final static int ClientCount = 2;
	final static int LinkCount = 2;
	final static int ServerCount = 2;
	final static int RoleCount = 2;

	private void areYouFight() throws InterruptedException {
		while (true) {
			for (var server : env.servers) {
				var fightModule = server.Zeze.getHotManager().getModuleContext("Game.Fight", IModuleFight.class);
				if (fightModule.getService().isAreYouFightDone())
					return;
			}
			//noinspection BusyWait
			Thread.sleep(1);
		}
	}

	@Test

	public void test3() throws Exception {
		Task.tryInitThreadPool();

		try {
			logger.info("=== test3 - start");
			// serverId 从 50 起排（旧组网沿用的编号，避开其他 Zezex 测试的 40 段），provider 端口仍从 20000 起排。
			env.prepareNewEnvironment(ClientCount, LinkCount, ServerCount, 50);

			// testcase first;
			logger.info("=== test3 - 1");
			var client0 = env.clients.getFirst();
			ZezexTestEnv.auth(client0.onLinkConnectedFuture.get(), client0, "account0");
			var role = ZezexTestEnv.getRole(client0);
			var roleId = null != role ? role.getId() : ZezexTestEnv.createRole(client0, "role0");
			ZezexTestEnv.login(client0, roleId);
			areYouFight();

			// testcase relogin
			logger.info("=== test3 - 2");
			client0.Stop();
			client0.Start("", 0); // loginQueue 不再需要link地址。
			ZezexTestEnv.auth(client0.onLinkConnectedFuture.get(), client0, "account0");
			relogin(client0, roleId);

			// testcase kick
			logger.info("=== test3 - 3");
			var client1 = env.clients.get(1);
			ZezexTestEnv.auth(client1.onLinkConnectedFuture.get(), client1, "account0");
			ZezexTestEnv.login(client1, roleId);

			// logout client1: client0 被踢了
			logger.info("=== test3 - 4");
			ZezexTestEnv.logout(client1, roleId);
		} catch (Throwable e) { // rethrow
			logger.error("", e);
			throw e;
		} finally {
			logger.info("=== test3 - stop");
			env.stopAll();
		}
	}

	private static void relogin(ClientGame.App app, long roleId) {
		var relogin = new ReLogin();
		relogin.Argument.setRoleId(roleId);
		relogin.SendForWait(app.ClientService.GetSocket(), 30_000).await();
		Assertions.assertEquals(0, relogin.getResultCode());
	}

//	public void testLoginXyz() throws Exception {
	// 理解 client-linkd-server 之间的关系，
	// 做好【准备工作】，分别做以下测试，并【验证结果】。
	// 【准备工作】
	// server需要在Online里面加一个role
	// 【验证结果】
	// 由于所有的服务都运行在同一个进程中，所以可以在做了某个操作以后，查询进程内服务的数据验证。
	// 【测试】
	// 第一 client-linkd-Auth（所有的和linkd的新连接都必须先完成这一步）
	// protocol      = Zezex.Linkd.Auth, 模块=
	// client.module = Zezex.Linkd.ModuleLinkd 需要写Send(Auth)，成功以后继续后面测试。采用异步方式。
	// linkd.module  = Zezex.Linkd.ModuleLinkd.ProcessAuthRequest 默认实现：任何账号都成功，一般不用改。
	// 第二 client-linkd-server-Login
	// protocol      = Zeze.Builtin.Game.Online.Login
	// client.module = Zeze.Builtin.Game.Online.ModuleOnline 需要写Send(Login)，异步成功以后，
	// server.module = Start过程中通过server.provider.Online注册Login事件，收到事件打印登录信息。
	// 第三 client-linkd-server-Logout
	// 基本上和第二步差不多，注册的事件是LogoutEvents。
	// 第四 client-linkd-server-Relogin
	// 不要做第三步的Logout，断开和Linkd的连接，然后重连成功以后，发送ReLogin。注册 ReloginEvents。
	// 第五 client-linkd-server-Kick
	// 完成一个client的Login后，再起一个client连接到跟它不同的linkd，然后auth&Login，观察Kick情况。
	// 1. 第一，第二，第三
	// 2. 第一，第二，第四
	// 3. 第一，第二，第五
	// 【注意】
	// 1. client对象管理。根据以上的几个测试，可能需要根据测试目的创建不同的client，分别选择特定的linkd进行连接。
	//    所以client一开始不用马上创建好，根据测试创建，上面的初始化流程就当作client的初始化例子吧。
//	}
}
