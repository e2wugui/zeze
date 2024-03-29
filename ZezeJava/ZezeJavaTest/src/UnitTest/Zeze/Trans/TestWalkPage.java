package UnitTest.Zeze.Trans;

import java.util.ArrayList;
import java.util.List;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DatabaseRedis;
import Zeze.Util.OutInt;
import demo.App;
import demo.Bean1;
import demo.Module1.tWalkPage;
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
		//demo.App.getInstance().Stop();
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
	public void testWalkPage_1() throws Exception {
		var t = TestWalkPage.prepareTable();
		ArrayList<Integer> walkedKeys = new ArrayList<>();
		var walkTimes = new OutInt(0);
		t.walk(((key, value) -> {
			walkTimes.value += 1;
			walkedKeys.add(key);
			return true;
		}));
		Assert.assertEquals(walkTimes.value, walkedKeys.size());
		var expected = List.of(1, 2, 3, 4, 5);
		if (t.getDatabase() instanceof DatabaseRedis) // unordered
			Assert.assertTrue(walkedKeys.containsAll(expected) && expected.containsAll(walkedKeys));
		else
			Assert.assertEquals(expected, walkedKeys);
	}

	@Test
	public void testWalkPage_2() throws Exception {
		var t = TestWalkPage.prepareTable();
		if (t.getDatabase() instanceof DatabaseRedis) // unsupported
			return;

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

	@Test
	public void testWalkPageDesc_1() throws Exception {
		var t = TestWalkPage.prepareTableDesc();
		if (t.getDatabase() instanceof DatabaseRedis) // unsupported
			return;

		ArrayList<Integer> walkedKeys = new ArrayList<>();
		var walkTimes = new OutInt(0);
		t.walkDesc(((key, value) -> {
			walkTimes.value += 1;
			walkedKeys.add(key);
			return true;
		}));
		Assert.assertEquals(walkTimes.value, walkedKeys.size());
		var expected = List.of(5, 4, 3, 2, 1);
		Assert.assertEquals(expected, walkedKeys);
	}

	@Test
	public void testWalkPageDesc_2() throws Exception {
		var t = TestWalkPage.prepareTableDesc();
		if (t.getDatabase() instanceof DatabaseRedis) // unsupported
			return;

		ArrayList<Integer> walkedKeys = new ArrayList<>();
		Integer exclusiveStartKey = null;
		var walkTimes = new OutInt(0);
		do {
			exclusiveStartKey = t.walkDesc(exclusiveStartKey, 1,
					(key, value) -> {
						walkTimes.value += 1;
						walkedKeys.add(key);
						return true;
					});
		} while (exclusiveStartKey != null);
		Assert.assertEquals(walkTimes.value, walkedKeys.size());
		var expected = List.of(5, 4, 3, 2, 1);
		Assert.assertEquals(expected, walkedKeys);
	}

	@Test
	public void testWalkKey() throws Exception {
		var t = TestWalkPage.prepareTable();
		if (t.getDatabase() instanceof DatabaseRedis) // unsupported
			return;

		ArrayList<Integer> walkedKeys = new ArrayList<>();
		var walkTimes = new OutInt(0);
		Integer exclusiveStartKey = null;
		do {
			exclusiveStartKey = t.walkKey(exclusiveStartKey, 1, (key) -> {
				walkTimes.value += 1;
				walkedKeys.add(key);
				return true;
			});
		} while (exclusiveStartKey != null);

		Assert.assertEquals(walkTimes.value, walkedKeys.size());
		var expected = List.of(1, 2, 3, 4, 5);
		Assert.assertEquals(expected, walkedKeys);
	}

	@Test
	public void testWalkKeyDesc() throws Exception {
		var t = TestWalkPage.prepareTableDesc();
		if (t.getDatabase() instanceof DatabaseRedis) // unsupported
			return;

		ArrayList<Integer> walkingKeys = new ArrayList<>();
		var walkTimes = new OutInt(0);
		Integer exclusiveStartKey = null;
		do {
			exclusiveStartKey = t.walkKeyDesc(exclusiveStartKey, 1, (key) -> {
				walkTimes.value += 1;
				walkingKeys.add(key);
				return true;
			});
		} while (exclusiveStartKey != null);
		Assert.assertEquals(walkTimes.value, walkingKeys.size());
		var expected = List.of(5, 4, 3, 2, 1);
		Assert.assertEquals(expected, walkingKeys);
	}

	@Test
	public void testWalkCacheKey() throws Exception {
		var t = TestWalkPage.prepareTable();
		ArrayList<Integer> walkedKeys = new ArrayList<>();
		var walkTimes = new OutInt(0);
		t.walkCacheKey(key -> {
			walkTimes.value += 1;
			walkedKeys.add(key);
			return true;
		});
		Assert.assertEquals(walkTimes.value, walkedKeys.size());
		var expected = List.of(1, 2, 3, 4, 5);
		Assert.assertEquals(expected, walkedKeys);
	}

	@Test
	public void testWalkDatabaseKey() throws Exception {
		var t = TestWalkPage.prepareTable();
		ArrayList<Integer> walkedKeys = new ArrayList<>();
		var walkTimes = new OutInt(0);
		t.walkDatabaseKey(key -> {
			walkTimes.value += 1;
			walkedKeys.add(key);
			return true;
		});

		Assert.assertEquals(walkTimes.value, walkedKeys.size());
		var expected = List.of(1, 2, 3, 4, 5);
		if (t.getDatabase() instanceof DatabaseRedis) // unordered
			Assert.assertTrue(walkedKeys.containsAll(expected) && expected.containsAll(walkedKeys));
		else
			Assert.assertEquals(expected, walkedKeys);
	}

	@Test
	public void testWalkDatabaseRaw() {
		var t = TestWalkPage.prepareTable();
		ArrayList<Integer> walkedKeys = new ArrayList<>();
		var walkTimes = new OutInt(0);

		if (!t.isUseRelationalMapping()) {
			t.walkDatabaseRaw((key, value) -> {
				var bbKey = t.decodeKey(key);
				walkTimes.value += 1;
				walkedKeys.add(bbKey);
				return true;
			});
			Assert.assertEquals(walkTimes.value, walkedKeys.size());
			var expected = List.of(1, 2, 3, 4, 5);
			if (t.getDatabase() instanceof DatabaseRedis) // unordered
				Assert.assertTrue(walkedKeys.containsAll(expected) && expected.containsAll(walkedKeys));
			else
				Assert.assertEquals(expected, walkedKeys);
		}
	}

	@Test
	public void testWalkDatabaseDescRaw() {
		var t = TestWalkPage.prepareTableDesc();
		if (t.getDatabase() instanceof DatabaseRedis) // unsupported
			return;

		ArrayList<Integer> walkedKeys = new ArrayList<>();
		var walkTimes = new OutInt(0);

		if (!t.isUseRelationalMapping()) {
			t.walkDatabaseRawDesc((key, value) -> {
				var bbKey = t.decodeKey(key);
				walkTimes.value += 1;
				walkedKeys.add(bbKey);
				return true;
			});
			Assert.assertEquals(walkTimes.value, walkedKeys.size());
			var expected = List.of(5, 4, 3, 2, 1);
			Assert.assertEquals(expected, walkedKeys);
		}
	}

	@Test
	public void testWalkDatabase() {
		var t = TestWalkPage.prepareTable();
		ArrayList<Integer> walkedKeys = new ArrayList<>();
		var walkTimes = new OutInt(0);

		t.walkDatabase((key, value) -> {
			walkTimes.value += 1;
			walkedKeys.add(key);
			return true;
		});

		Assert.assertEquals(walkTimes.value, walkedKeys.size());
		var expected = List.of(1, 2, 3, 4, 5);
		if (t.getDatabase() instanceof DatabaseRedis) // unordered
			Assert.assertTrue(walkedKeys.containsAll(expected) && expected.containsAll(walkedKeys));
		else
			Assert.assertEquals(expected, walkedKeys);
	}

	@Test
	public void testWalkDatabaseDesc() {
		var t = TestWalkPage.prepareTableDesc();
		if (t.getDatabase() instanceof DatabaseRedis) // unsupported
			return;

		ArrayList<Integer> walkedKeys = new ArrayList<>();
		var walkTimes = new OutInt(0);

		t.walkDatabaseDesc((key, value) -> {
			walkTimes.value += 1;
			walkedKeys.add(key);
			return true;
		});

		Assert.assertEquals(walkTimes.value, walkedKeys.size());
		var expected = List.of(5, 4, 3, 2, 1);
		Assert.assertEquals(expected, walkedKeys);
	}

	@Test
	public void testWalkCache() {
		var t = TestWalkPage.prepareTable();
		ArrayList<Integer> walkedKeys = new ArrayList<>();
		var walkTimes = new OutInt(0);
		t.walkMemory((key, value) -> {
			walkTimes.value += 1;
			walkedKeys.add(key);
			return true;
		});
	}

	private static tWalkPage prepareTable() {
		var t = App.getInstance().demo_Module1.tWalkPage();
		App.getInstance().Zeze.newProcedure(() -> {
			t.put(5, new Bean1());
			t.put(3, new Bean1());
			t.put(1, new Bean1());
			t.put(2, new Bean1());
			t.put(4, new Bean1());
			return 0;
		}, "prepare walk data").call();
		App.getInstance().Zeze.checkpointRun();
		return t;
	}

	private static tWalkPage prepareTableDesc() {
		var t = App.getInstance().demo_Module1.tWalkPage();
		App.getInstance().Zeze.newProcedure(() -> {
			t.put(3, new Bean1());
			t.put(2, new Bean1());
			t.put(1, new Bean1());
			t.put(5, new Bean1());
			t.put(4, new Bean1());
			return 0;
		}, "prepare walk data by desc").call();
		App.getInstance().Zeze.checkpointRun();
		return t;
	}
}
