package UnitTest.Zeze.Trans;

import java.lang.reflect.InvocationTargetException;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;
import demo.App;
import demo.Module1.BValue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestCheckpoint{

	@Before
	public final void testInit() throws Throwable {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Throwable {
		demo.App.getInstance().Stop();
	}

	@Test
	public final void testModeTable() throws Throwable {
		/*
		Assert.assertEquals(Procedure.Success, App.Instance.Zeze.NewProcedure(() -> {
			App.Instance.demo_Module1.getTable1().remove(1L);
			App.Instance.demo_Module1.getTable1().remove(2L);
			App.Instance.demo_Module1.getTable1().remove(3L);
			App.Instance.demo_Module1.getTable1().remove(4L);
			return 0L;
		}, "remove").call());
		System.out.println("1");
		System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		App.Instance.Zeze.CheckpointRun();
		System.out.println("2");
		System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		*/
		Assert.assertEquals(Procedure.Success, App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.getTable1().get(1L);
			App.Instance.demo_Module1.getTable1().getOrAdd(2L).setInt1(222);
			return 0L;
		}, "12").call());
		//System.out.println("3");
		//System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		Assert.assertEquals(Procedure.Success, App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.getTable1().get(3L);
			App.Instance.demo_Module1.getTable1().getOrAdd(4L).setInt1(444);
			return 0L;
		}, "34").call());
		//System.out.println("4");
		//System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		Assert.assertEquals(Procedure.Success, App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.getTable1().get(2L);
			App.Instance.demo_Module1.getTable1().getOrAdd(3L).setInt1(333);
			return 0L;
		}, "23").call());
		//System.out.println("5");
		//System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		App.Instance.Zeze.checkpointRun();
		//System.out.println("6");
		//System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());

		var table = demo.App.getInstance().demo_Module1.getTable1();
		var dbtable = table.internalGetStorageForTestOnly("IKnownWhatIAmDoing").getDatabaseTable();
		Assert.assertNotNull(dbtable.find(table.encodeKey(2L)));
		Assert.assertNotNull(dbtable.find(table.encodeKey(4L)));
		Assert.assertNotNull(dbtable.find(table.encodeKey(3L)));
	}

	@Test
	public final void testCp() throws Throwable {
		Assert.assertEquals(demo.App.getInstance().Zeze.newProcedure(TestCheckpoint::ProcClear, "ProcClear").call(), Procedure.Success);
		Assert.assertEquals(demo.App.getInstance().Zeze.newProcedure(this::ProcChange, "ProcChange").call(), Procedure.Success);
		demo.App.getInstance().Zeze.checkpointRun();
		demo.Module1.Table1 table = demo.App.getInstance().demo_Module1.getTable1();
		ByteBuffer value = table.internalGetStorageForTestOnly("IKnownWhatIAmDoing").getDatabaseTable().find(table.encodeKey(56L));
		Assert.assertNotNull(value);
		Assert.assertEquals(resetVersion(value), resetVersion(bytesInTrans));
	}

	private ByteBuffer resetVersion(ByteBuffer bb) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		var value = new BValue();
		value.decode(bb);
		var method = value.getClass().getDeclaredMethod("version", long.class);
		method.setAccessible(true);
		method.invoke(value, 0);
		var result = ByteBuffer.Allocate();
		value.encode(result);
		return result;
	}

	private static long ProcClear() {
		demo.App.getInstance().demo_Module1.getTable1().remove(56L);
		return Procedure.Success;
	}

	private ByteBuffer bytesInTrans;
	private long ProcChange() {
		BValue v = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(56L);
		v.setInt1(1);
		bytesInTrans = ByteBuffer.Allocate();
		v.encode(bytesInTrans);
		return Procedure.Success;
	}
}
