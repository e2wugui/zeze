package Zeze.Serialize;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestRawBeanExtendedField {
	@Test
	void preservesAnExtendedFirstFieldAndStopsAtTheBeanBoundary() {
		for (int fieldId : new int[]{15, 16, 143, 16399}) {
			var encoded = ByteBuffer.Allocate();
			encoded.WriteTag(0, fieldId, IByteBuffer.INTEGER);
			encoded.WriteLong(123456);
			encoded.WriteTag(fieldId, fieldId + 1, IByteBuffer.BYTES);
			encoded.WriteString("kept");
			encoded.WriteByte(0);
			byte[] expected = encoded.Copy();
			encoded.WriteInt(42);
			for (IByteBuffer input : new IByteBuffer[]{ByteBuffer.Wrap(encoded.Copy()),
					NioByteBuffer.Wrap(encoded.Copy())}) {
				var raw = new RawBean(123);
				raw.decode(input);
				assertArrayEquals(expected, raw.getRawData().toBytes());
				assertArrayEquals(expected, ByteBuffer.encode(raw).Copy());
				assertEquals(42, input.ReadInt());
			}
		}
	}
}
