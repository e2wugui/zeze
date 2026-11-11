package Zeze.Transaction;

import org.jspecify.annotations.Nullable;

import org.jspecify.annotations.NullMarked;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;
import demo.App;
import demo.Module1.BValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("DataFlowIssue")
@NullMarked
public class TestCheckpoint {
	@BeforeEach
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@AfterEach
	public final void testCleanup() throws Exception {
		//demo.App.getInstance().Stop();
	}

	@Test
	public final void testModeTable() throws Exception {
		/*
		Assertions.assertEquals(Procedure.Success, App.Instance.Zeze.NewProcedure(() -> {
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
		Assertions.assertEquals(Procedure.Success, App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.getTable1().get(1L);
			App.Instance.demo_Module1.getTable1().getOrAdd(2L).setInt_1(222);
			return 0L;
		}, "12").call());
		//System.out.println("3");
		//System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		Assertions.assertEquals(Procedure.Success, App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.getTable1().get(3L);
			App.Instance.demo_Module1.getTable1().getOrAdd(4L).setInt_1(444);
			return 0L;
		}, "34").call());
		//System.out.println("4");
		//System.out.println(Zeze.Transaction.RelativeRecordSet.RelativeRecordSetMapToString());
		Assertions.assertEquals(Procedure.Success, App.Instance.Zeze.newProcedure(() -> {
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
		Assertions.assertNotNull(awaitFind(table, 2L));
		Assertions.assertNotNull(awaitFind(table, 4L));
		Assertions.assertNotNull(awaitFind(table, 3L));
	}

	@Test
	public final void testCp() throws Exception {
		Assertions.assertEquals(Procedure.Success, App.getInstance().Zeze.newProcedure(TestCheckpoint::ProcClear, "ProcClear").call());
		Assertions.assertEquals(Procedure.Success, App.getInstance().Zeze.newProcedure(this::ProcChange, "ProcChange").call());
		demo.App.getInstance().Zeze.checkpointRun();
		demo.Module1.Table1 table = demo.App.getInstance().demo_Module1.getTable1();
		var bValueTrans = new BValue();
		bValueTrans.decode(ByteBuffer.Wrap(bytesInTrans));
		var want = bytesOf(resetVersion(bValueTrans));
		// dbh2等网络后端：checkpointRun()返回时remove+put的2pc落库仍在途（Memory同步写无此窗口），
		// 且共享库的key56常有他测残留行——"非空"不构成flush已落库的判据，须轮询至**内容收敛**
		// （soak-20261111-1954八轮稳定读回残留行、fresh库批soak-20261112-0322读null，同窗两形态）。
		// 超时未收敛=flush真失败/键被悬挂事务锁死，是诚实的红。
		var dbtable = table.internalGetStorageForTestOnly("IKnownWhatIAmDoing").getDatabaseTable();
		var deadline = System.currentTimeMillis() + 30_000;
		byte[] got = null;
		for (;;) {
			var value = dbtable.find(table, 56L);
			if (value != null) {
				got = bytesOf(resetVersion(value));
				if (java.util.Arrays.equals(want, got))
					break;
			}
			if (System.currentTimeMillis() > deadline)
				break;
			Thread.sleep(100);
		}
		Assertions.assertArrayEquals(want, got);
	}

	/** resetVersion产物的紧凑字节视图（ByteBuffer首尾索引截取），供逐字节比较。 */
	private static byte[] bytesOf(ByteBuffer bb) {
		return java.util.Arrays.copyOfRange(bb.Bytes, bb.ReadIndex, bb.ReadIndex + bb.size());
	}

	/**
	 * dbh2等网络后端：checkpointRun()返回时2pc落库仍在途（Memory同步写无此窗口）——
	 * 直接storage读须有界轮询至可见。fresh库对照批soak-20261112-0322实锤find=null。
	 */
	private static BValue awaitFind(demo.Module1.Table1 table, long key) throws InterruptedException {
		var dbtable = table.internalGetStorageForTestOnly("IKnownWhatIAmDoing").getDatabaseTable();
		var deadline = System.currentTimeMillis() + 30_000;
		BValue value;
		while ((value = dbtable.find(table, key)) == null && System.currentTimeMillis() < deadline)
			Thread.sleep(100);
		return value;
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

	private @Nullable ByteBuffer bytesInTrans;

	private long ProcChange() {
		BValue v = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(56L);
		v.setInt_1(1);
		bytesInTrans = ByteBuffer.Allocate();
		v.encode(bytesInTrans);
		return Procedure.Success;
	}
}
