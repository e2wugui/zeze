package Zeze.Serialize;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Data;
import Zeze.Transaction.DynamicData;
import Zeze.Transaction.EmptyBean;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestDynamicDataBeanCompatibility {
	@Test
	public void assigningAnotherWrapperUsesTheDestinationPayloadType() {
		var source = new TestDynamicData(false);
		var payload = new ValueData();
		payload.value = 42;
		source.setData(payload);

		var target = new TestDynamicData(false, 8);
		target.setData(source);

		assertEquals(7, source.getTypeId());
		assertEquals(8, target.getTypeId());
		assertSame(payload, target.getData());

		var encoded = ByteBuffer.Allocate();
		target.encode(encoded);
		var decoded = new TestDynamicData(false, 8);
		decoded.decode(encoded);
		assertEquals(42, assertInstanceOf(ValueData.class, decoded.getData()).value);
		assertTrue(encoded.isEmpty());
	}


	@Test
	public void compatibleBeanInstallsItsDecodedDataAndActualType() {
		var target = new TestDynamicData(true);
		target.newData(7);
		var input = encodedValue();
		assertSame(target, input.ReadDynamic(target, ByteBuffer.BEAN));
		assertEquals(7, target.getTypeId());
		assertEquals(42, assertInstanceOf(ValueData.class, target.getData()).value);
		assertTrue(input.isEmpty());
	}

	@Test
	public void absentCompatibilityFactoryUsesTheNormalEmptyFallback() {
		var target = new TestDynamicData(false);
		target.newData(7);
		var input = encodedValue();
		input.ReadDynamic(target, ByteBuffer.BEAN);
		assertTrue(target.isEmpty());
		assertTrue(input.isEmpty());
	}

	private static ByteBuffer encodedValue() {
		var input = ByteBuffer.Allocate();
		input.WriteTag(0, 1, ByteBuffer.INTEGER);
		input.WriteInt(42);
		input.WriteByte(0);
		return input;
	}

	private static final class TestDynamicData extends DynamicData {
		private final boolean compatible;
		private final long payloadType;

		private TestDynamicData(boolean compatible) {
			this(compatible, 7);
		}

		private TestDynamicData(boolean compatible, long payloadType) {
			this.compatible = compatible;
			this.payloadType = payloadType;
		}

		@Override
		public long toTypeId(Data data) {
			return data instanceof ValueData ? payloadType : EmptyBean.TYPEID;
		}

		@Override
		public @Nullable Data toData(long typeId) {
			return typeId == payloadType || (compatible && typeId == 0) ? new ValueData() : null;
		}
	}

	private static final class ValueData extends Data {
		private int value;

		@Override
		public void assign(Bean bean) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Bean toBean() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void reset() {
			value = 0;
		}

		@Override
		public Data copy() {
			var copy = new ValueData();
			copy.value = value;
			return copy;
		}

		@Override
		public void encode(ByteBuffer output) {
			output.WriteTag(0, 1, ByteBuffer.INTEGER);
			output.WriteInt(value);
			output.WriteByte(0);
		}

		@Override
		public void decode(IByteBuffer input) {
			value = input.ReadInt(input.ReadByte());
			input.skipAllUnknownFields(input.ReadByte());
		}
	}
}
