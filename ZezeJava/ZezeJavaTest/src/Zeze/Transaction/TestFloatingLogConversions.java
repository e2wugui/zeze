package Zeze.Transaction;

import java.util.ArrayList;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Logs.LogDoubleValue;
import Zeze.Transaction.Logs.LogFloatValue;
import Zeze.Transaction.Logs.LogLongValue;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestFloatingLogConversions {

	@Test
	public void floatReplayConversionsMatchRecordDecoding() {
		var checks = new ArrayList<Executable>();
		for (float value : new float[]{0.5f, -0.5f, 0x1p40f, -0x1p40f, Float.NaN,
				Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0, -0.0f, 127, -128}) {
			var input = ByteBuffer.Allocate();
			input.WriteFloat(value);
			var log = LogFloatValue.decode(1, input);
			checks.add(() -> verifyConversions(log, ByteBuffer.FLOAT));
		}
		assertAll(checks);
	}

	@Test
	public void doubleReplayConversionsMatchRecordDecoding() {
		var checks = new ArrayList<Executable>();
		for (double value : new double[]{0.5, -0.5, 0x1p40, -0x1p40, Double.NaN,
				Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0, -0.0, 32767, -32768}) {
			var input = ByteBuffer.Allocate();
			input.WriteDouble(value);
			var log = LogDoubleValue.decode(1, input);
			checks.add(() -> verifyConversions(log, ByteBuffer.DOUBLE));
		}
		assertAll(checks);
	}

	@Test
	public void integralReplayConversionsKeepTheirLowBits() {
		for (long value : new long[]{0, 1, -1, 0x1_0000_0000L, Long.MIN_VALUE, Long.MAX_VALUE}) {
			var input = ByteBuffer.Allocate();
			input.WriteLong(value);
			verifyConversions(LogLongValue.decode(1, input), ByteBuffer.INTEGER);
		}
	}

	private static void verifyConversions(Log log, int type) {
		var encoded = ByteBuffer.Allocate();
		log.encode(encoded);
		var bytes = encoded.Copy();
		assertAll(log.toString(),
				() -> assertEquals(ByteBuffer.Wrap(bytes).ReadBool(type), log.booleanValue()),
				() -> assertEquals(ByteBuffer.Wrap(bytes).ReadByte(type), log.byteValue()),
				() -> assertEquals(ByteBuffer.Wrap(bytes).ReadShort(type), log.shortValue()),
				() -> assertEquals(ByteBuffer.Wrap(bytes).ReadInt(type), log.intValue()),
				() -> assertEquals(ByteBuffer.Wrap(bytes).ReadLong(type), log.longValue()));
	}
}
