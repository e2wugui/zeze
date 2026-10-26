package Zeze.Transaction.Collections;

import harness.Fast;
import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.NioByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Collections.LogList1.OpLog;

@Fast
public class TestListLogStructure {
	private static IByteBuffer input(ByteBuffer bytes, boolean nio) {
		return nio ? NioByteBuffer.Wrap(java.nio.ByteBuffer.wrap(bytes.Copy())) : ByteBuffer.Wrap(bytes.Copy());
	}

	private static void decode(boolean bean, IByteBuffer input) {
		if (bean)
			LogList2.decode(0, List2Meta.get(EmptyBean.class), input);
		else
			LogList1.decode(0, List1Meta.get(Integer.class), input);
	}

	@Test
	public void unknownOperationsFailAtTheOperationBoundary() {
		var checks = new ArrayList<Executable>();
		for (boolean bean : new boolean[]{false, true})
			for (boolean nio : new boolean[]{false, true})
				for (int operation : new int[]{4, 127, -1, Integer.MIN_VALUE})
					checks.add(() -> {
						var bytes = ByteBuffer.Allocate();
						if (bean)
							bytes.WriteUInt(0);
						bytes.WriteUInt(1);
						bytes.WriteUInt(operation);
						int boundary = bytes.getWriteIndex();
						bytes.WriteInt(0);
						bytes.WriteInt(0);
						var input = input(bytes, nio);
						Assertions.assertThrows(IllegalStateException.class, () -> decode(bean, input),
								"bean=" + bean + ", nio=" + nio + ", operation=" + operation);
						Assertions.assertEquals(boundary, input.getReadIndex());
					});
		Assertions.assertAll(checks);
	}

	@Test
	public void unsignedIndexesOutsideIntRangeFailBeforeTheValue() {
		var checks = new ArrayList<Executable>();
		for (boolean bean : new boolean[]{false, true})
			for (boolean nio : new boolean[]{false, true})
				for (int operation : new int[]{OpLog.OP_ADD, OpLog.OP_MODIFY, OpLog.OP_REMOVE})
					for (int index : new int[]{-1, Integer.MIN_VALUE})
						checks.add(() -> {
							var bytes = ByteBuffer.Allocate();
							if (bean)
								bytes.WriteUInt(0);
							bytes.WriteUInt(1);
							bytes.WriteUInt(operation);
							bytes.WriteUInt(index);
							int boundary = bytes.getWriteIndex();
							bytes.WriteInt(0);
							var input = input(bytes, nio);
							Assertions.assertThrows(IllegalStateException.class, () -> decode(bean, input));
							Assertions.assertEquals(boundary, input.getReadIndex());
						});
		Assertions.assertAll(checks);
	}

	@Test
	public void beanChangeIndexesMustAlsoBeNonnegative() {
		for (boolean nio : new boolean[]{false, true}) {
			for (int index : new int[]{-1, Integer.MIN_VALUE}) {
				var bytes = ByteBuffer.Allocate();
				bytes.WriteUInt(1);
				LogMap2.encodeLogBean(bytes, new LogBean(0, null));
				bytes.WriteUInt(index);
				int boundary = bytes.getWriteIndex();
				bytes.WriteUInt(0);
				var input = input(bytes, nio);
				Assertions.assertThrows(IllegalStateException.class, () -> decode(true, input));
				Assertions.assertEquals(boundary, input.getReadIndex());
			}
		}
	}

	@Test
	public void validOperationSequenceKeepsItsWireEncodingAndReplay() {
		var scalar = new LogList1<>(0, null, List1Meta.get(Integer.class), null);
		scalar.getOpLogs().add(new OpLog<>(OpLog.OP_ADD, 0, 11));
		scalar.getOpLogs().add(new OpLog<>(OpLog.OP_MODIFY, 0, 22));
		scalar.getOpLogs().add(new OpLog<>(OpLog.OP_ADD, 1, 33));
		scalar.getOpLogs().add(new OpLog<>(OpLog.OP_REMOVE, 0, null));
		scalar.getOpLogs().add(new OpLog<>(OpLog.OP_CLEAR, 0, null));
		scalar.getOpLogs().add(new OpLog<>(OpLog.OP_ADD, 0, 44));
		var beans = new LogList2<>(0, null, List2Meta.get(EmptyBean.class), null, null);
		for (var operation : scalar.getOpLogs())
			beans.getOpLogs().add(new OpLog<>(operation.op, operation.index,
					operation.value != null ? new EmptyBean() : null));
		for (boolean nio : new boolean[]{false, true}) {
			var bytes = ByteBuffer.Allocate();
			scalar.encode(bytes);
			var input = input(bytes, nio);
			var decoded = LogList1.decode(0, List1Meta.get(Integer.class), input);
			Assertions.assertTrue(input.isEmpty());
			var encoded = ByteBuffer.Allocate();
			decoded.encode(encoded);
			Assertions.assertArrayEquals(bytes.Copy(), encoded.Copy());
			var list = new PList1<>(Integer.class);
			list.followerApply(decoded);
			Assertions.assertEquals(java.util.List.of(44), new ArrayList<>(list));

			bytes = ByteBuffer.Allocate();
			beans.encode(bytes);
			input = input(bytes, nio);
			var beanLog = LogList2.decode(0, List2Meta.get(EmptyBean.class), input);
			Assertions.assertTrue(input.isEmpty());
			encoded = ByteBuffer.Allocate();
			beanLog.encode(encoded);
			Assertions.assertArrayEquals(bytes.Copy(), encoded.Copy());
			var beanList = new PList2<>(EmptyBean.class);
			beanList.followerApply(beanLog);
			Assertions.assertEquals(1, beanList.size());
		}
	}
}
