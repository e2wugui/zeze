package UnitTest.Zeze.Component;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;
import demo.App;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestAutoKey {
	@Before
	public final void testInit() throws Exception {
		System.out.println("testInit");
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Exception {
		System.out.println("testCleanup");
		demo.App.getInstance().Stop();
	}

	private static long makeId(long index) {
		var bb = ByteBuffer.Allocate(8);
		var serverId = App.getInstance().Zeze.getConfig().getServerId();
		if (serverId > 0)
			bb.WriteUInt(serverId);
		bb.WriteULong(index);
		return ByteBuffer.ToLongBE(bb.Bytes, bb.ReadIndex, bb.Size());
	}

	@Test
	public final void test1_AutoKey() throws Exception {
		System.out.println("testAutoKey1");
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			var autoKey = demo.App.getInstance().Zeze.getAutoKeyAtomic("test1");
			var id = autoKey.nextId();
			Assert.assertEquals(makeId(1), id);
			return Procedure.Success;
		}, "test1_AutoKey").call());
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			var autoKey = demo.App.getInstance().Zeze.getAutoKeyAtomic("test1");
			var id = autoKey.nextId();
			Assert.assertEquals(makeId(2), id);
			return Procedure.Success;
		}, "test1_AutoKey").call());
	}

	@Test
	public final void test2_AutoKey() throws Exception {
		System.out.println("testAutoKey2");
		var allocCount = demo.App.getInstance().Zeze.getAutoKeyAtomic("test1").getAllocateCount();
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			var autoKey = demo.App.getInstance().Zeze.getAutoKeyAtomic("test1");
			var id = autoKey.nextId();
			Assert.assertEquals(makeId(allocCount + 1), id);
			return Procedure.Success;
		}, "test2_AutoKey").call());
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			var autoKey = demo.App.getInstance().Zeze.getAutoKeyAtomic("test1");
			var id = autoKey.nextId();
			Assert.assertEquals(makeId(allocCount + 2), id);
			return Procedure.Success;
		}, "test2_AutoKey").call());
	}

	@Test
	public final void test3_AutoKey() throws Exception {
		System.out.println("testAutoKey2");
		var allocCount = demo.App.getInstance().Zeze.getAutoKeyAtomic("test1").getAllocateCount();
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			var autoKey = demo.App.getInstance().Zeze.getAutoKeyAtomic("test1");
			var id = autoKey.nextId();
			Assert.assertEquals(makeId(allocCount * 2L + 1), id);
			return Procedure.Success;
		}, "test3_AutoKey").call());
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			var autoKey = demo.App.getInstance().Zeze.getAutoKeyAtomic("test1");
			var id = autoKey.nextId();
			Assert.assertEquals(makeId(allocCount * 2L + 2), id);
			return Procedure.Success;
		}, "test3_AutoKey").call());
	}
}
