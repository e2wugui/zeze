package Zezex;

import Zeze.Net.AsyncSocket;
import junit.framework.TestCase;

public class TestOnline extends TestCase {
	Client.App client1;
	Zezex.App link1;
	Game.App  server1;
	Game.App  server2;

	@Override
	protected void setUp() {
		client1 = Client.App.Instance;
		link1 = App.Instance;
		server1 = Game.App.Instance;
		server2 = new Game.App();

		try {
			link1.Start();
			server1.Start(new String[]{"-ServerId", "0"});
			server2.Start(new String[]{"-ServerId", "1", "-ProviderDirectPort", "20002"});

			Thread.sleep(2000); // wait server register ready
			client1.Start();
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	protected void tearDown() {
		System.out.println("Begin Stop");
		try {
			client1.Stop();
			server1.Stop();
			server2.Stop();
			link1.Stop();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		System.out.println("End Stop");
	}

	public void testNow() throws Throwable {

	}
}
