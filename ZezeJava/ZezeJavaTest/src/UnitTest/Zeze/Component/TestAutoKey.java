package UnitTest.Zeze.Component;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;
import demo.App;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestAutoKey {
	@BeforeEach
	public final void testInit() throws Exception {
		System.out.println("testInit");
		demo.App.getInstance().Stop();
		demo.App.getInstance().Start();
	}

	@AfterEach
	public final void testCleanup() throws Exception {
		//System.out.println("testCleanup");
		//demo.App.getInstance().Stop();
	}

	private static long makeId(long index) {
		var bb = ByteBuffer.Allocate(8);
		var serverId = App.getInstance().Zeze.getConfig().getServerId();
		Assertions.assertTrue(serverId >= 0);
		if (serverId > 0)
			bb.WriteUInt(serverId);
		bb.WriteULong(index);
		Assertions.assertTrue(bb.size() <= 8);
		return ByteBuffer.ToLongBE(bb.Bytes, bb.ReadIndex, bb.size());
	}

	@SuppressWarnings("deprecation")
	@Test
	public final void test1_AutoKey() {
		Assertions.assertTrue(demo.App.getInstance().Zeze.getAutoKey("test1").resetSeedUnsafe());
		System.out.println("testAutoKey1");
		Assertions.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			var autoKey = demo.App.getInstance().Zeze.getAutoKey("test1");
			var id = autoKey.nextId();
			Assertions.assertEquals(makeId(1), id);
			return Procedure.Success;
		}, "test1_AutoKey").call());
		Assertions.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			var autoKey = demo.App.getInstance().Zeze.getAutoKey("test1");
			var id = autoKey.nextId();
			Assertions.assertEquals(makeId(2), id);
			return Procedure.Success;
		}, "test1_AutoKey").call());
	}

	@Test
	public final void test2_AutoKey() {
		System.out.println("testAutoKey2");
		var allocCount = demo.App.getInstance().Zeze.getAutoKey("test1").getAllocateCount();
		Assertions.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			var autoKey = demo.App.getInstance().Zeze.getAutoKey("test1");
			var id = autoKey.nextId();
			Assertions.assertEquals(makeId(allocCount + 1), id);
			return Procedure.Success;
		}, "test2_AutoKey").call());
		Assertions.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			var autoKey = demo.App.getInstance().Zeze.getAutoKey("test1");
			var id = autoKey.nextId();
			Assertions.assertEquals(makeId(allocCount + 2), id);
			return Procedure.Success;
		}, "test2_AutoKey").call());
	}

	@Test
	public final void test3_AutoKey() {
		System.out.println("testAutoKey2");
		var allocCount = demo.App.getInstance().Zeze.getAutoKey("test1").getAllocateCount();
		Assertions.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			var autoKey = demo.App.getInstance().Zeze.getAutoKey("test1");
			var id = autoKey.nextId();
			Assertions.assertEquals(makeId(allocCount * 2L + 1), id);
			return Procedure.Success;
		}, "test3_AutoKey").call());
		Assertions.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			var autoKey = demo.App.getInstance().Zeze.getAutoKey("test1");
			var id = autoKey.nextId();
			Assertions.assertEquals(makeId(allocCount * 2L + 2), id);
			return Procedure.Success;
		}, "test3_AutoKey").call());
	}
}
