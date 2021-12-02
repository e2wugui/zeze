package UnitTest.Zeze.Trans;

import demo.App;
import org.junit.After;
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
		assert App.Instance.Zeze.NewProcedure(() -> {
			App.Instance.demo_Module1.getTable1().remove(1L);
			App.Instance.demo_Module1.getTable1().remove(2L);
			App.Instance.demo_Module1.getTable1().remove(3L);
			App.Instance.demo_Module1.getTable1().remove(4L);
			return 0L;
		}, "remove").Call() == Procedure.Success;
		System.out.println("1");
		System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		App.Instance.Zeze.CheckpointRun();
		System.out.println("2");
		System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		*/
		assert App.Instance.Zeze.NewProcedure(() -> {
			App.Instance.demo_Module1.getTable1().get(1L);
			App.Instance.demo_Module1.getTable1().getOrAdd(2L).setInt1(222);
			return 0L;
		}, "12").Call() == Procedure.Success;
		//System.out.println("3");
		//System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		assert App.Instance.Zeze.NewProcedure(() -> {
			App.Instance.demo_Module1.getTable1().get(3L);
			App.Instance.demo_Module1.getTable1().getOrAdd(4L).setInt1(444);
			return 0L;
		}, "34").Call() == Procedure.Success;
		//System.out.println("4");
		//System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		assert App.Instance.Zeze.NewProcedure(() -> {
			App.Instance.demo_Module1.getTable1().get(2L);
			App.Instance.demo_Module1.getTable1().getOrAdd(3L).setInt1(333);
			return 0L;
		}, "23").Call() == Procedure.Success;
		//System.out.println("5");
		//System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		App.Instance.Zeze.CheckpointRun();
		//System.out.println("6");
		//System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());

		var table = demo.App.getInstance().demo_Module1.getTable1();
		var dbtable = table.InternalGetStorageForTestOnly("IKnownWhatIAmDoing").getDatabaseTable();
		assert null != dbtable.Find(table.EncodeKey(2L));
		assert null != dbtable.Find(table.EncodeKey(4L));
		assert null != dbtable.Find(table.EncodeKey(3L));
	}

	@Test
	public final void testCp() throws Throwable {
		assert demo.App.getInstance().Zeze.NewProcedure(this::ProcClear, "ProcClear").Call() == Procedure.Success;
		assert demo.App.getInstance().Zeze.NewProcedure(this::ProcChange, "ProcChange").Call() == Procedure.Success;
		demo.App.getInstance().Zeze.CheckpointRun();
		demo.Module1.Table1 table = demo.App.getInstance().demo_Module1.getTable1();
		ByteBuffer value = table.InternalGetStorageForTestOnly("IKnownWhatIAmDoing").getDatabaseTable().Find(table.EncodeKey(56L));
		assert value != null;
		assert value.equals(bytesInTrans);
	}

	private long ProcClear() {
		demo.App.getInstance().demo_Module1.getTable1().remove(56L);
		return Procedure.Success;
	}

	private ByteBuffer bytesInTrans;
	private long ProcChange() {
		demo.Module1.Value v = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(56L);
		v.setInt1(1);
		bytesInTrans = ByteBuffer.Allocate();
		v.Encode(bytesInTrans);
		return Procedure.Success;
	}
}