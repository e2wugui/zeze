package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 尾数为零的大指数词是零（JDK parseDouble("0e999")==0）：数值解析的
 * 指数溢出分支只看指数不看尾数，0e999按分隔符路径返回Infinity/
 * Integer.MAX_VALUE/Long.MAX_VALUE——同一个词在缓冲区中间与末尾
 * （EOF路径走正常回退）得到不同结果。用于限额、时长等字段时
 * 零被静默变成极值。
 */
@Fast
public class TestJsonZeroWithHugeExponent {

	private static final JsonReader jr = new JsonReader();

	@Test
	public void zeroMantissaHugeExponentIsZeroOnAllPaths() {
		// 分隔符路径（词后跟空格/}）与EOF路径（词在缓冲区末尾）必须一致为0
		for (var text : new String[]{"0e999 ", "0e999", "0e999}", "-0e999", "0.0e999", "0e3090"}) {
			var d = jr.buf(text).parseDouble();
			assertEquals(Double.parseDouble(text.replace("}", "").trim()), d, 0.0,
					"double路径: " + text + " 实际=" + d);
			assertEquals(0, jr.buf(text).parseInt(), "int路径: " + text);
			assertEquals(0L, jr.buf(text).parseLong(), "long路径: " + text);
		}
	}

	@Test
	public void nonZeroMantissaHugeExponentStillSaturates() {
		Assertions.assertEquals(Double.POSITIVE_INFINITY, jr.buf("1e999 ").parseDouble());
		Assertions.assertEquals(Integer.MAX_VALUE, jr.buf("1e999 ").parseInt());
		Assertions.assertEquals(Long.MAX_VALUE, jr.buf("1e999 ").parseLong());
		Assertions.assertEquals(0.05, jr.buf("0.5e-1 ").parseDouble(), 1e-12, "小指数不受影响");
	}
}
