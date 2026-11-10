package Zeze.Transaction;

import Zeze.History.Helper;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogBean;
import demo.Module1.BSimple;
import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestLogDynamicRoundTrip {
	static {
		Helper.registerLogs();
		// 先初始化生成类，再读取静态字段声明。
		LogRegistry.registerDynamicFields(new BValue().getClass(), __ -> {}, __ -> {});
	}

	@Test
	public void testValueRoundTrip() {
		var parent = new BValue();
		var dynamic = BValue.newDynamicBean_Dynamic14();
		dynamic.initRootInfo(newRootInfo(), parent);
		var value = new demo.Bean1();
		value.setV1(42);
		var log = new LogDynamic(parent, 14, dynamic);
		log.setValue(BValue.DynamicTypeId_Dynamic14_demo_Bean1, value);

		assertRoundTrip(log);
	}

	@Test
	public void usesTypeIdForFieldAndTagForNestedLog() {
		var parent = new BValue();
		var dynamic = BValue.newDynamicBean_Dynamic14();
		var log = new LogDynamic(parent, 14, dynamic);
		log.setValue(BValue.DynamicTypeId_Dynamic14_demo_Bean1, new demo.Bean1());

		var parentLog = new LogBean(0, parent);
		parentLog.getVariablesOrNew().put(14, log);
		var fieldBytes = ByteBuffer.Wrap(encode(parentLog));
		assertEquals(1, fieldBytes.ReadUInt());
		assertEquals(Bean.hash32("Zeze.Transaction.LogDynamic"), fieldBytes.ReadInt4());
		assertRoundTrip(parentLog);

		var nestedBytes = ByteBuffer.Allocate();
		LogBean.encodeLogBean(nestedBytes, log);
		assertEquals(1, nestedBytes.Bytes[nestedBytes.ReadIndex]);
		assertArrayEquals(encode(log), encode(LogBean.decodeLogBean(nestedBytes)));
	}

	@Test
	public void testLogBeanRoundTrip() {
		var parent = new BValue();
		var dynamic = BValue.newDynamicBean_Dynamic14();
		var inner = new BValue();
		dynamic.bean = inner;
		dynamic.initRootInfo(newRootInfo(), parent);
		var log = new LogDynamic(parent, 14, dynamic);
		var innerLog = new LogBean(0, inner);
		innerLog.getVariablesOrNew().put(2, Map2TestSupport.longLog(2, 7));
		log.addChild(dynamic, innerLog);

		assertRoundTrip(log);
	}

	@Test
	public void testEmptyRoundTrip() {
		var parent = new BValue();
		var dynamic = BValue.newDynamicBean_Dynamic14();
		dynamic.initRootInfo(newRootInfo(), parent);

		assertRoundTrip(new LogDynamic(parent, 14, dynamic));
	}

	@Test
	public void testDecodedCollectionElementRoundTrip() {
		var parent = new BValue();
		var dynamic = parent.getList43().createValue();
		parent.getList43().add(dynamic);
		parent.initRootInfo(newRootInfo(), null);
		var value = new BSimple();
		value.setInt_1(43);
		var log = new LogDynamic(parent.getList43(), dynamic.variableId(), dynamic);
		log.setValue(BValue.getSpecialTypeIdFromBean_43(value), value);

		assertRoundTrip(log);
	}

	@Test
	public void testDecodedLogIsPayloadOnly() {
		var parent = new BValue();
		var dynamic = BValue.newDynamicBean_Dynamic14();
		dynamic.initRootInfo(newRootInfo(), parent);
		var source = new LogDynamic(parent, 14, dynamic);
		var decoded = LogRegistry.decode(source.getTypeId(), 14, ByteBuffer.Wrap(encode(source)));

		assertFalse(decoded instanceof TransactionLog);
	}

	@Test
	public void testWriterRequiresTransactionTarget() {
		var parent = new BValue();
		var dynamic = BValue.newDynamicBean_Dynamic14();
		dynamic.initRootInfo(newRootInfo(), parent);

		// 直接传 null，验证构造器能拒绝绕过静态检查的调用。
		assertThrows(NullPointerException.class, () -> new LogDynamic(null, 14, dynamic));
		assertThrows(NullPointerException.class, () -> new LogDynamic(parent, 14, null));
	}

	@Test
	public void testParentlessDynamicRejectsLogCreationAndReplacement() {
		var dynamic = BValue.newDynamicBean_Dynamic14();
		dynamic.initRootInfo(newRootInfo(), null);
		var value = new demo.Bean1();

		var error = assertThrows(IllegalStateException.class, () -> dynamic.setBean(value));
		assertEquals("DynamicBean has no parent for its transaction log", error.getMessage());
		assertFalse(value.isManaged());
		assertThrows(IllegalStateException.class, () -> dynamic.createLogBean(null));
	}

	@Test
	public void testLogWithoutReplacementKeepsBeanAndType() {
		var parent = new BValue();
		var dynamic = BValue.newDynamicBean_Dynamic14();
		var value = new demo.Bean1();
		dynamic.setBean(value);
		dynamic.initRootInfo(newRootInfo(), parent);
		var txn = Transaction.create(new Locks());
		txn.begin();
		try {
			txn.putLog(new LogDynamic(parent, dynamic.variableId(), dynamic));
			assertSame(value, dynamic.getBean());
			assertEquals(BValue.DynamicTypeId_Dynamic14_demo_Bean1, dynamic.getTypeId());
		} finally {
			Transaction.destroy();
		}
	}

	private static void assertRoundTrip(Log source) {
		var first = encode(source);
		var decoded = LogRegistry.decode(source.getTypeId(), source.getVariableId(), ByteBuffer.Wrap(first));
		var second = encode(decoded);
		assertArrayEquals(first, second);
	}

	private static byte[] encode(Log log) {
		var bb = ByteBuffer.Allocate();
		log.encode(bb);
		return bb.getBytes(0, bb.size());
	}

	private static Record.RootInfo newRootInfo() {
		var record = new Record(null) {
			@Override
			public Table getTable() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Object getObjectKey() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setDirty() {
			}

			@Override
			public IGlobalAgent.AcquireResult acquire(int state, boolean fresh, boolean noWait) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void commit(RecordAccessed accessed) {
			}
		};
		return new Record.RootInfo(record, new TableKey(1, "TestLogDynamicRoundTrip"));
	}
}
