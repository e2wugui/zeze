package UnitTest.Zeze.Trans;

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
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Exception {
		//demo.App.getInstance().Stop();
	}

	@Test
	public final void testModeTable() throws Exception {
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
			App.Instance.demo_Module1.getTable1().getOrAdd(2L).setInt_1(222);
			return 0L;
		}, "12").call());
		//System.out.println("3");
		//System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		Assert.assertEquals(Procedure.Success, App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.getTable1().get(3L);
			App.Instance.demo_Module1.getTable1().getOrAdd(4L).setInt_1(444);
			return 0L;
		}, "34").call());
		//System.out.println("4");
		//System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		Assert.assertEquals(Procedure.Success, App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.getTable1().get(2L);
			App.Instance.demo_Module1.getTable1().getOrAdd(3L).setInt_1(333);
			return 0L;
		}, "23").call());
		//System.out.println("5");
		//System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		App.Instance.Zeze.checkpointRun();
		//System.out.println("6");
		//System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());

		var table = demo.App.getInstance().demo_Module1.getTable1();
		var dbtable = table.internalGetStorageForTestOnly("IKnownWhatIAmDoing").getDatabaseTable();
		Assert.assertNotNull(dbtable.find(table, 2L));
		Assert.assertNotNull(dbtable.find(table, 4L));
		Assert.assertNotNull(dbtable.find(table, 3L));
	}

	@Test
	public final void testCp() throws Exception {
		Assert.assertEquals(demo.App.getInstance().Zeze.newProcedure(TestCheckpoint::ProcClear, "ProcClear").call(), Procedure.Success);
		Assert.assertEquals(demo.App.getInstance().Zeze.newProcedure(this::ProcChange, "ProcChange").call(), Procedure.Success);
		demo.App.getInstance().Zeze.checkpointRun();
		demo.Module1.Table1 table = demo.App.getInstance().demo_Module1.getTable1();
		var value = table.internalGetStorageForTestOnly("IKnownWhatIAmDoing").getDatabaseTable().find(table, 56L);
		Assert.assertNotNull(value);
		var bValueTrans = new BValue();
		bValueTrans.decode(ByteBuffer.Wrap(bytesInTrans));
		Assert.assertEquals(resetVersion(value), resetVersion(bValueTrans));
	}

	private static ByteBuffer resetVersion(BValue value) throws ReflectiveOperationException {
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
		v.setInt_1(1);
		bytesInTrans = ByteBuffer.Allocate();
		v.encode(bytesInTrans);
		return Procedure.Success;
	}
}
