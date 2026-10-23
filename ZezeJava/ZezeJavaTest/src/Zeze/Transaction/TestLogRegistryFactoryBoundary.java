package Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Consumer;

import Zeze.History.Helper;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Quaternion;
import Zeze.Serialize.Vector2;
import Zeze.Serialize.Vector2Int;
import Zeze.Serialize.Vector3;
import Zeze.Serialize.Vector3Int;
import Zeze.Serialize.Vector4;
import Zeze.Transaction.Logs.LogBeanKeyValue;
import Zeze.Transaction.Logs.LogIntValue;
import Zeze.Util.Id128;
import harness.Fast;
import org.junit.jupiter.api.Test;

@Fast
public class TestLogRegistryFactoryBoundary {
	private static final int TYPE_ID = Bean.hash32(TestLogRegistryFactoryBoundary.class.getName());

	private static final class Value extends Log {
		private final int value;

		Value(int id, int value) {
			super(id);
			this.value = value;
		}

		@Override
		public int getTypeId() {
			return TYPE_ID;
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteInt(value);
		}
	}

	@Test
	public void replacementRequiresExactFactoryOwnership() {
		LogRegistry.LogDecoder decoder = (id, input) -> new Value(id, input.ReadInt());
		var owner = new LogRegistry.Factory(TYPE_ID, "Value", decoder);
		LogRegistry.register(owner);
		var lookalike = new LogRegistry.Factory(TYPE_ID, "Value", decoder);
		var replacement = new LogRegistry.Factory(TYPE_ID, "Value", decoder);
		assertEquals(owner, lookalike);
		assertFalse(LogRegistry.replaceRegistered(TYPE_ID, lookalike, replacement));
		assertSame(owner, LogRegistry.getRegistered(TYPE_ID));
		assertTrue(LogRegistry.replaceRegistered(TYPE_ID, owner, replacement));
		assertSame(replacement, LogRegistry.getRegistered(TYPE_ID));
		var bb = ByteBuffer.Allocate();
		bb.WriteInt(7);
		var decoded = (Value)LogRegistry.decode(TYPE_ID, 2, bb);
		assertEquals(7, decoded.value);
		assertEquals(2, decoded.getVariableId());
	}

	@Test
	public void decodedScalarCannotEnterLocalTransaction() {
		var bb = ByteBuffer.Allocate();
		bb.WriteInt(7);
		Log decoded = LogIntValue.decode(2, bb);
		assertFalse(decoded instanceof TransactionLog);
		assertEquals(7, decoded.intValue());
		assertEquals("LogInt", decoded.getTypeName());
	}

	private static void assertScalar(String protocolType, String typeName, Consumer<ByteBuffer> writeValue) {
		var input = ByteBuffer.Allocate();
		writeValue.accept(input);
		var bytes = input.Copy();
		var typeId = Bean.hash32("Zeze.Transaction.Log<" + protocolType + ">");
		var decoded = LogRegistry.decode(typeId, 3, input);
		assertFalse(decoded instanceof TransactionLog, typeName);
		assertEquals(typeName + "Value", decoded.getClass().getSimpleName());
		assertEquals(typeName, decoded.getTypeName());
		assertEquals(typeId, decoded.getTypeId());
		assertEquals(3, decoded.getVariableId());
		assertEquals(0, input.size());
		var encoded = ByteBuffer.Allocate();
		decoded.encode(encoded);
		assertArrayEquals(bytes, encoded.Copy(), typeName);
	}

	@Test
	public void scalarRegistryKeepsProtocolAndReturnsOnlyValues() {
		Helper.registerLogs();
		assertScalar("bool", "LogBool", bb -> bb.WriteBool(true));
		assertScalar("byte", "LogByte", bb -> bb.WriteLong(-7));
		assertScalar("short", "LogShort", bb -> bb.WriteInt(-700));
		assertScalar("int", "LogInt", bb -> bb.WriteInt(-70_000));
		assertScalar("long", "LogLong", bb -> bb.WriteLong(Long.MIN_VALUE));
		assertScalar("float", "LogFloat", bb -> bb.WriteFloat(7.25f));
		assertScalar("double", "LogDouble", bb -> bb.WriteDouble(-7.25));
		assertScalar("binary", "LogBinary", bb -> bb.WriteBinary(new Binary(new byte[]{0, 7, -1})));
		assertScalar("string", "LogString", bb -> bb.WriteString("Count = 7; 背包"));
		assertScalar("decimal", "LogDecimal", bb -> bb.WriteString("1234567890123456789012345678901234567890.0700"));
		assertScalar("vector2", "LogVector2", bb -> bb.WriteVector2(new Vector2(1, 7)));
		assertScalar("vector2int", "LogVector2Int", bb -> bb.WriteVector2Int(new Vector2Int(1, 7)));
		assertScalar("vector3", "LogVector3", bb -> bb.WriteVector3(new Vector3(1, 2, 7)));
		assertScalar("vector3int", "LogVector3Int", bb -> bb.WriteVector3Int(new Vector3Int(1, 2, 7)));
		assertScalar("vector4", "LogVector4", bb -> bb.WriteVector4(new Vector4(1, 2, 3, 7)));
		assertScalar("quaternion", "LogQuaternion", bb -> bb.WriteQuaternion(new Quaternion(1, 2, 3, 7)));
	}

	@Test
	public void beanKeyReplayAcceptsValueWithoutTransactionBinding() {
		Helper.registerLogBeanKey(Id128.class);
		var bean = new Zeze.Builtin.HistoryModule.BLogChanges();
		var value = new Id128(7, 1001);
		var input = ByteBuffer.Allocate();
		value.encode(input);
		var meta = Zeze.Transaction.Collections.BeanKeyMeta.get(Id128.class);
		var decoded = LogRegistry.decode(meta.logTypeId, 1, input);
		assertInstanceOf(LogBeanKeyValue.class, decoded);
		assertFalse(decoded instanceof TransactionLog);
		var changes = new Zeze.Transaction.Collections.LogBean(0, null);
		changes.getVariablesOrNew().put(1, decoded);
		bean.followerApply(changes);
		assertEquals(value, bean.getGlobalSerialId());
	}
}
