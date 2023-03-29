package UnitTest.Zeze.Trans;

import java.util.ArrayList;
import java.util.List;
import Zeze.Util.OutInt;
import demo.App;
import demo.Bean1;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestWalkPage {
	@Before
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Exception {
		demo.App.getInstance().Stop();
	}

	@Test
	public void testFind() throws Exception {
		var t = App.Instance.demo_Module1.getTable1();
		App.Instance.Zeze.newProcedure(() -> {
			t.getOrAdd(1L).setInt_1(1);
			return 0;
		}, "find").call();
	}

	@Test
	public void testWalkPage() throws Exception {
		var t = App.getInstance().demo_Module1.tWalkPage();
		App.getInstance().Zeze.newProcedure(() -> {
			t.put(1, new Bean1());
			t.put(2, new Bean1());
			t.put(3, new Bean1());
			t.put(4, new Bean1());
			t.put(5, new Bean1());
			return 0;
		}, "prepare walk data").call();
		App.getInstance().Zeze.checkpointRun();

		var walkedKeys = new ArrayList<Integer>();
		Integer exclusiveStartKey = null;
		var walkTimes = new OutInt(0);
		do {
			exclusiveStartKey = t.walk(exclusiveStartKey, 1,
					(key, value) -> {
						walkTimes.value += 1;
						walkedKeys.add(key);
						return true;
					});
		} while (exclusiveStartKey != null);
		Assert.assertEquals(walkTimes.value, walkedKeys.size());
		var expected = List.of(1, 2, 3, 4, 5);
		Assert.assertEquals(expected, walkedKeys);
	}
}
