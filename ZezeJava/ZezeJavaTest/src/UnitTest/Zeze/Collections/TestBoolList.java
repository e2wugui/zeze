package UnitTest.Zeze.Collections;

import Zeze.Collections.BoolList;
import Zeze.Util.OutInt;
import demo.App;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestBoolList {
	@Before
	public void before() throws Exception {
		App.Instance.Start();
	}

	@Test
	public void testBoolList() throws Exception {
		var bl = App.Instance.BoolListModule.open("myBoolListTest");
		for (int i = 0; i < 1024; ++i) {
			Assert.assertFalse(get(bl, i));
		}
		App.Instance.Zeze.newProcedure(() -> {
			for (int i = 0; i < 1024; ++i) {
				bl.set(i);
			}
			return 0;
		}, "set all").call();
		for (int i = 0; i < 1024; ++i) {
			Assert.assertTrue(get(bl, i));
		}
		App.Instance.Zeze.newProcedure(() -> {
			for (int i = 0; i < 1024; ++i) {
				bl.clear(i);
			}
			return 0;
		}, "set all").call();
		for (int i = 0; i < 1024; ++i) {
			Assert.assertFalse(get(bl, i));
		}
	}

	private static boolean get(BoolList bl, int index) {
		var out = new OutInt();
		App.Instance.Zeze.newProcedure(() -> {
			out.value = bl.get(index) ? 1 : 0;
			return 0;
		}, "get one").call();
		return out.value == 1;
	}
}
