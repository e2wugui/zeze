package UnitTest.Zeze.Game;

import Game.App;
import junit.framework.TestCase;

public class TestRank extends TestCase {
	private App app1;
	private App app2;

	@Override
	protected void setUp() {
		app1 = App.Instance;
		app2 = new App();

		try {
			app1.Start(new String[]{"-ServerId", "0"});
			app2.Start(new String[]{"-ServerId", "1", "-ProviderDirectPort", "20002"});

			System.out.println("Begin Thread.sleep");
			Thread.sleep(2000); // wait connected
			System.out.println("End Thread.sleep app1 " + app1.Zeze.getServiceManagerAgent().getSubscribeStates().values());
			System.out.println("End Thread.sleep app2 " + app1.Zeze.getServiceManagerAgent().getSubscribeStates().values());
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void tearDown() {
		System.out.println("Begin Stop");
		try {
			app1.Stop();
			app2.Stop();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		System.out.println("End Stop");
	}

	public void testRank() throws Throwable {
		//TODO
	}
}
