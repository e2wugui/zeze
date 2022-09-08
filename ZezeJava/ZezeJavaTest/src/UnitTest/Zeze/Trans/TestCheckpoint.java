package UnitTest.Zeze.Trans;

import demo.App;
import demo.Module1.BValue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;

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
		}, "remove").Call());
		System.out.println("1");
		System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		App.Instance.Zeze.CheckpointRun();
		System.out.println("2");
		System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		*/
		Assert.assertEquals(Procedure.Success, App.Instance.Zeze.NewProcedure(() -> {
			App.Instance.demo_Module1.getTable1().get(1L);
			App.Instance.demo_Module1.getTable1().getOrAdd(2L).setInt1(222);
			return 0L;
		}, "12").Call());
		//System.out.println("3");
		//System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		Assert.assertEquals(Procedure.Success, App.Instance.Zeze.NewProcedure(() -> {
			App.Instance.demo_Module1.getTable1().get(3L);
			App.Instance.demo_Module1.getTable1().getOrAdd(4L).setInt1(444);
			return 0L;
		}, "34").Call());
		//System.out.println("4");
		//System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		Assert.assertEquals(Procedure.Success, App.Instance.Zeze.NewProcedure(() -> {
			App.Instance.demo_Module1.getTable1().get(2L);
			App.Instance.demo_Module1.getTable1().getOrAdd(3L).setInt1(333);
			return 0L;
		}, "23").Call());
		//System.out.println("5");
		//System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		App.Instance.Zeze.CheckpointRun();
		//System.out.println("6");
		//System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());

		var table = demo.App.getInstance().demo_Module1.getTable1();
		var dbtable = table.InternalGetStorageForTestOnly("IKnownWhatIAmDoing").getDatabaseTable();
		Assert.assertNotNull(dbtable.Find(table.EncodeKey(2L)));
		Assert.assertNotNull(dbtable.Find(table.EncodeKey(4L)));
		Assert.assertNotNull(dbtable.Find(table.EncodeKey(3L)));
	}

	@Test
	public final void testCp() throws Throwable {
		Assert.assertEquals(demo.App.getInstance().Zeze.NewProcedure(TestCheckpoint::ProcClear, "ProcClear").Call(), Procedure.Success);
		Assert.assertEquals(demo.App.getInstance().Zeze.NewProcedure(this::ProcChange, "ProcChange").Call(), Procedure.Success);
		demo.App.getInstance().Zeze.CheckpointRun();
		demo.Module1.Table1 table = demo.App.getInstance().demo_Module1.getTable1();
		ByteBuffer value = table.InternalGetStorageForTestOnly("IKnownWhatIAmDoing").getDatabaseTable().Find(table.EncodeKey(56L));
		Assert.assertNotNull(value);
		Assert.assertEquals(value, bytesInTrans);
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
		v.Encode(bytesInTrans);
		return Procedure.Success;
	}
}
