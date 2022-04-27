package Zezex;

import java.util.ArrayList;
import Zeze.Net.AsyncSocket;
import io.perfmark.Link;
import junit.framework.TestCase;

public class TestOnline extends TestCase {
	ArrayList<Client.App> clients = new ArrayList<>();
	ArrayList<Zezex.App> links = new ArrayList<>();
	ArrayList<Game.App>  servers = new ArrayList<>();

	final int ClientCount = 2;
	final int LinkCount = 2;
	final int ServerCount = 2;

	@Override
	protected void setUp() {
		for (int i = 0; i < ClientCount; ++i)
			clients.add(new Client.App());
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
	}

	@Override
	protected void tearDown() {
		System.out.println("Begin Stop");
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
		System.out.println("End Stop");
	}

	public void testLoginXyz() throws Throwable {
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
	}
}
