package UnitTest.Zeze.Collections;

import Zeze.Collections.BoolList;
import Zeze.Util.OutInt;
import demo.App;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestBoolList {
	@BeforeEach
	public void before() throws Exception {
		App.Instance.Start();
	}

	@Test
	public void testBoolList() {
		var bl = App.Instance.BoolListModule.open("myBoolListTest");
		for (int i = 0; i < 1024; ++i) {
			Assertions.assertFalse(get(bl, i));
		}
		App.Instance.Zeze.newProcedure(() -> {
			for (int i = 0; i < 1024; ++i) {
				bl.set(i);
			}
			return 0;
		}, "set all").call();
		for (int i = 0; i < 1024; ++i) {
			Assertions.assertTrue(get(bl, i));
		}
		App.Instance.Zeze.newProcedure(() -> {
			for (int i = 0; i < 1024; ++i) {
				bl.clear(i);
			}
			return 0;
		}, "set all").call();
		for (int i = 0; i < 1024; ++i) {
			Assertions.assertFalse(get(bl, i));
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
