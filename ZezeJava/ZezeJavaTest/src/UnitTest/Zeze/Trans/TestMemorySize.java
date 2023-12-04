package UnitTest.Zeze.Trans;

import demo.App;
import demo.Bean1;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestMemorySize {
	@Before
	public void before() throws Exception {
		demo.App.getInstance().Start();
	}

	@Test
	public void testMemorySize() throws Exception {
		Assert.assertEquals(0, App.Instance.demo_Module1.tMemorySize().getCacheSize());
		App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.tMemorySize().insert(1L, new Bean1());
			return 0;
		}, "Insert").call();
		Assert.assertEquals(1, App.Instance.demo_Module1.tMemorySize().getCacheSize());
		App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.tMemorySize().insert(2L, new Bean1());
			return 0;
		}, "Insert").call();
		Assert.assertEquals(2, App.Instance.demo_Module1.tMemorySize().getCacheSize());
		App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.tMemorySize().remove(2L);
			return 0;
		}, "Insert").call();
		Assert.assertEquals(1, App.Instance.demo_Module1.tMemorySize().getCacheSize());
		App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.tMemorySize().remove(1L);
			return 0;
		}, "Insert").call();
		Assert.assertEquals(0, App.Instance.demo_Module1.tMemorySize().getCacheSize());
	}

	@Test
	public void testMemoryRollback() {
		Assert.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.tMemorySize().remove(9L);
			return 0;
		}, "testMemoryRollback1").call());

		var oldSize = App.Instance.demo_Module1.tMemorySize().getCacheSize();
		Assert.assertEquals(1, App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.tMemorySize().getOrAdd(9L);
			return 1;
		}, "testMemoryRollback2").call());
		var newSize = App.Instance.demo_Module1.tMemorySize().getCacheSize();
		Assert.assertEquals(oldSize, newSize);
	}

	@Test
	public void testMemoryCache() {
		Assert.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.tMemorySize().remove(8L);
			return 0;
		}, "testMemoryCache").call());

		var oldSize = App.Instance.demo_Module1.tMemorySize().getCacheSize();
		for (int i = 0; i < 0x40; i++) {
			int j = i;
			int r = i & 1;
			Assert.assertEquals(r, App.Instance.Zeze.newProcedure(() -> {
				if (r == 1) {
					Bean1 v = null;
					switch ((j >> 1) & 3) {
					case 0:
						v = App.Instance.demo_Module1.tMemorySize().get(8L);
						break;
					case 1:
						v = App.Instance.demo_Module1.tMemorySize().getOrAdd(8L);
						break;
					case 2:
						v = new Bean1(11);
						App.Instance.demo_Module1.tMemorySize().tryAdd(8L, v);
						break;
					case 3:
						v = new Bean1(22);
						App.Instance.demo_Module1.tMemorySize().put(8L, v);
						break;
					}
					if (v != null && ((j >> 3) & 1) == 1)
						v.setV1(123);
				} else
					App.Instance.demo_Module1.tMemorySize().get(8L);
				if (((j >> 4) & 1) == 1)
					App.Instance.demo_Module1.tMemorySize().get(8L);
				if (((j >> 5) & 1) == 1)
					App.Instance.demo_Module1.tMemorySize().remove(8L);
				return r;
			}, "testMemoryCache" + i).call());
			Assert.assertEquals(oldSize, App.Instance.demo_Module1.tMemorySize().getCacheSize());
		}
	}
}
