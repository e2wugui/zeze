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

		private TestDynamicData(boolean compatible) {
			this.compatible = compatible;
		}

		@Override
		public long toTypeId(Data data) {
			return data instanceof ValueData ? 7 : EmptyBean.TYPEID;
		}

		@Override
		public @Nullable Data toData(long typeId) {
			return typeId == 7 || (compatible && typeId == 0) ? new ValueData() : null;
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
