package Zezex;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import Game.Login.BRole;
import Game.Login.CreateRole;
import Zeze.Builtin.Game.Online.Login;
import Zeze.Builtin.Game.Online.Logout;
import Zeze.Builtin.Game.Online.ReLogin;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import Zezex.Linkd.Auth;
import junit.framework.TestCase;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

public class TestOnline extends TestCase {
	static {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(Level.INFO);
	}

	private static final Logger logger = LogManager.getLogger(TestOnline.class);

	final ArrayList<ClientGame.App> clients = new ArrayList<>();
	final ArrayList<Zezex.App> links = new ArrayList<>();
	final ArrayList<Game.App>  servers = new ArrayList<>();
	ArrayList<BRole> roles = new ArrayList<>();
	ArrayList<String> accounts = new ArrayList<>();

	final static int ClientCount = 2;
	final static int LinkCount = 2;
	final static int ServerCount = 2;
	final static int RoleCount = 2;

	@Override
	protected void setUp() throws ExecutionException, InterruptedException {
		Task.tryInitThreadPool(null, null, null);

		for (int i = 0; i < ClientCount; ++i) {
			var client = new ClientGame.App();
			clients.add(client);
		}
		for (int i = 0; i < LinkCount; ++i)
			links.add(new Zezex.App());
		for (int i = 0; i < ServerCount; ++i)
			servers.add(new Game.App());

		try {
			for (int i = 0; i < LinkCount; ++i)
				links.get(i).Start(10000 + i, 15000 + i);
			for (int i = 0; i < ServerCount; ++i)
				servers.get(i).Start(i, 20000 + i);

			Thread.sleep(2000); // wait server ready

			for (int i = 0; i < ClientCount; ++i) {
				var link = links.get(i % LinkCount); // 按顺序选择link
				var ipport = link.LinkdService.GetOnePassiveAddress();
				clients.get(i).Start(ipport.getKey(), ipport.getValue());
			}
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}

		for (int i = 0; i < RoleCount; i++) {
			String name = "RoleName_" + i;

			var so = clients.get(i).ClientService.GetSocket();

			CreateRole createRole = new CreateRole();
			createRole.Argument.setName(name);
			var role = createRole.SendForWait(so).get();
			roles.add(role);

			String account = "RoleAccount_" + i;
			servers.get(i % servers.size()).provider.Online.addRole(account, role.getId());
			accounts.add(account);
		}
	}

	@Override
	protected void tearDown() {
		logger.info("Begin Stop");
		try {
			for (var client : clients)
				client.Stop();
			for (var server : servers)
				server.Stop();
			for (var link : links)
				link.Stop();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		roles = null;
		accounts = null;
		logger.info("End Stop");
	}

//	public void testLoginXyz() throws Throwable {
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

	public void testLogin() {
		logger.info("login test start");
		for (int i = 0; i < RoleCount; ++i) {
			BRole role = roles.get(i);
			String account = accounts.get(i);
			long roleId = role.getId();
			var socket = clients.get(i).ClientService.GetSocket();
			servers.get(i % servers.size()).provider.Online.addRole(account, roleId);

			Auth auth = new Auth();
			auth.Argument.setAccount(account);
			auth.SendForWait(socket).await();
			if (auth.getResultCode() != Procedure.Success) {
				logger.error("role index {} id {} auth error {}", i, roleId, auth.getResultCode());
				break;
			}

			Login login = new Login();
			login.Argument.setRoleId(roleId);
			login.SendForWait(socket).await();
			if (login.getResultCode() != Procedure.Success) {
				logger.error("role index {} id {} login error {}", i, roleId, login.getResultCode());
				break;
			}
		}
	}

	public void testLogout() {
		logger.info("logout test start");
		for (int i = 0; i < roles.size(); i++) {
			BRole role = roles.get(i);
			String account = accounts.get(i);
			long roleId = role.getId();
			var socket = clients.get(i).ClientService.GetSocket();
			Auth auth = new Auth();
			auth.Argument.setAccount(account);
			auth.SendForWait(socket).await();
			if (auth.getResultCode() != Procedure.Success) {
				logger.error("role index {} id {} auth error {}", i, roleId, auth.getResultCode());
				break;
			}

			Login login = new Login();
			login.Argument.setRoleId(roleId);
			login.SendForWait(socket).await();
			if (login.getResultCode() != Procedure.Success) {
				logger.error("role index {} id {} login error {}", i, roleId, login.getResultCode());
				break;
			}

			Logout logout = new Logout();
			logout.SendForWait(socket).await();
			if (logout.getResultCode() != Procedure.Success) {
				logger.error("role index {} id {} logout error {}", i, roleId, login.getResultCode());
				break;
			}
		}
		logger.info("logout test end");
	}

	public void testRelogin() {
		logger.info("relogin test start");
		for (int i = 0; i < roles.size(); i++) {
			BRole role = roles.get(i);
			String account = accounts.get(i);
			long roleId = role.getId();
			var socket = clients.get(i).ClientService.GetSocket();
			Auth auth = new Auth();
			auth.Argument.setAccount(account);
			auth.SendForWait(socket).await();
			if (auth.getResultCode() != Procedure.Success) {
				logger.error("role index {} id {} auth error {}", i, roleId, auth.getResultCode());
				break;
			}

			Login login = new Login();
			login.Argument.setRoleId(roleId);
			login.SendForWait(socket).await();
			if (login.getResultCode() != Procedure.Success) {
				logger.error("role index {} id {} login error {}", i, roleId, login.getResultCode());
				break;
			}

			ReLogin relogin = new ReLogin();
			relogin.Argument.setRoleId(roleId);
			relogin.SendForWait(socket).await();
			if (relogin.getResultCode() != Procedure.Success) {
				logger.error("role index {} id {} relogin error {}", i, roleId, relogin.getResultCode());
				break;
			}
		}
		logger.info("relogin test end");
	}

	public void testKick() {
		logger.info("kick test start");
		BRole role = roles.get(0);
		String account = accounts.get(0);
		long roleId = role.getId();
		var socket = clients.get(0).ClientService.GetSocket();
		Auth auth = new Auth();
		auth.Argument.setAccount(account);
		auth.SendForWait(socket).await();
		if (auth.getResultCode() != Procedure.Success) {
			logger.error("role id {} auth error {}", roleId, auth.getResultCode());
			return;
		}

		Login login = new Login();
		login.Argument.setRoleId(roleId);
		login.SendForWait(socket).await();
		if (login.getResultCode() != Procedure.Success) {
			logger.error("role id {} login error {}", roleId, login.getResultCode());
			return;
		}

		Auth auth1 = new Auth();
		auth1.Argument.setAccount(account);
		auth1.SendForWait(socket).await();
		if (auth1.getResultCode() != Procedure.Success) {
			logger.error("role id {} auth1 error {}", roleId, auth1.getResultCode());
			return;
		}

		Login login1 = new Login();
		login1.Argument.setRoleId(roleId);
		login1.SendForWait(socket).await();
		if (login1.getResultCode() != Procedure.Success) {
			logger.error("role id {} login1 error {}", roleId, login1.getResultCode());
			return;
		}
		logger.info("kick test end");
	}
}
