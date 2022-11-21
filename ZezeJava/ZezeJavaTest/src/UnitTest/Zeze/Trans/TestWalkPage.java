package UnitTest.Zeze.Trans;

import demo.App;
import demo.Bean1;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestWalkPage extends TestCase {
	@Before
	public final void testInit() throws Throwable {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Throwable {
		demo.App.getInstance().Stop();
	}

	@Test
	public void testWalkPage() throws Throwable {
		var t = App.getInstance().demo_Module1.tWalkPage();
		App.getInstance().Zeze.newProcedure(() -> {
			t.put(1, new Bean1());
			t.put(2, new Bean1());
			t.put(3, new Bean1());
			t.put(4, new Bean1());
			t.put(5, new Bean1());
			return 0;
		}, "prepare walk data").call();

		t.walk(null, 1,
				(key, value) -> {
				
					return true;
			});
	}
}
