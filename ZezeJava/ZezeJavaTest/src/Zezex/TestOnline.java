package Zezex;

import Zeze.Net.AsyncSocket;
import junit.framework.TestCase;

public class TestOnline extends TestCase {
	Zezex.App link1;
	Game.App  server1;
	Game.App  server2;

	@Override
	protected void setUp() {
		link1 = App.Instance;
		server1 = Game.App.Instance;
		server2 = new Game.App();

		try {
			link1.Start();
			server1.Start(new String[]{"-ServerId", "0"});
			server2.Start(new String[]{"-ServerId", "1", "-ProviderDirectPort", "20002"});

			Thread.sleep(2000); // wait server register ready
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	protected void tearDown() {
		System.out.println("Begin Stop");
		try {
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
