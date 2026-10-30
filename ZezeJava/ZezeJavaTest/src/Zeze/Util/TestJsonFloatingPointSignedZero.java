package Zeze.Util;

import java.util.List;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestJsonFloatingPointSignedZero {
	@Test
	void preservesNegativeZeroAcrossIntegerAndFractionalLexemes() {
		for (var token : List.of("-0", "-0.0", "-0e0", "-0x0")) {
			for (var suffix : List.of("", ",next")) {
				var reader = new JsonReader().buf(token + suffix);
				assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(reader.parseDouble()), token);
				assertEquals(token.length(), reader.pos());
			}
		}
		for (var token : List.of("0", "+0", "0.0", "0e0", "42", "-42", "9007199254740993"))
			assertEquals(Double.doubleToRawLongBits(Double.parseDouble(token)),
					Double.doubleToRawLongBits(new JsonReader().buf(token).parseDouble()), token);
	}

	public static class Values {
		public double scalar;
		public float single;
		public Double boxed;
		public double[] array;
		public List<Double> list;
	}

	@Test
	void typedFloatingPointValuesKeepTheZeroSign() {
		assertEquals(Double.doubleToRawLongBits(-0.0),
				Double.doubleToRawLongBits(Json.parse("-0", Double.class)));
		var values = Json.parse("{scalar:-0,single:-0,boxed:-0,array:[-0],list:[-0]}", Values.class);
		assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(values.scalar));
		assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(values.single));
		assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(values.boxed));
		assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(values.array[0]));
		assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(values.list.get(0)));
	}
}
