package UnitTest.Zeze.Trans;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;

public class TestCheckpoint{
	
	@Before
	public final void testInit() {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() {
		demo.App.getInstance().Stop();
	}

	@Test
	public final void testCp() {
		assert demo.App.getInstance().Zeze.NewProcedure(this::ProcClear, "ProcClear", null).Call() == Procedure.Success;
		assert demo.App.getInstance().Zeze.NewProcedure(this::ProcChange, "ProcChange", null).Call() == Procedure.Success;
		demo.App.getInstance().Zeze.CheckpointRun();
		demo.Module1.Table1 table = demo.App.getInstance().demo_Module1.getTable1();
		ByteBuffer value = table.InternalGetStorageForTestOnly("IKnownWhatIAmDoing").getDatabaseTable().Find(table.EncodeKey(56L));
		assert value != null;
		assert value.equals(bytesInTrans);
	}

	private int ProcClear() {
		demo.App.getInstance().demo_Module1.getTable1().remove(56L);
		return Procedure.Success;
	}

	private ByteBuffer bytesInTrans;
	private int ProcChange() {
		demo.Module1.Value v = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(56L);
		v.setInt1(1);
		bytesInTrans = ByteBuffer.Allocate();
		v.Encode(bytesInTrans);
		return Procedure.Success;
	}
}