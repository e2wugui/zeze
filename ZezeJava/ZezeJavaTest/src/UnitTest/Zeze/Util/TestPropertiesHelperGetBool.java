package UnitTest.Zeze.Util;

import harness.Fast;
import Zeze.Util.PropertiesHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * getBool 显式校验（FND2-U2-3）：
 * Boolean.parseBoolean 只识别忽略大小写的"true"、其余一律返回false且从不抛异常，
 * 原实现的 catch(NumberFormatException) 是死代码——垃圾值(-Dx=yes)被静默当成false，
 * 既无 warn 也丢失默认值。修复后仅接受"true"/"false"（忽略大小写），非法值 warn 并返回默认值。
 */
@Fast
public class TestPropertiesHelperGetBool {

	@Test
	public void testGetBool() {
		var key = "UnitTest.Zeze.Util.TestPropertiesHelperGetBool.flag";
		try {
			// 未设置 / 空 / 空白 → 默认值
			Assertions.assertTrue(PropertiesHelper.getBool(key, true));
			Assertions.assertFalse(PropertiesHelper.getBool(key, false));
			for (var blank : new String[]{"", " ", "\t"}) {
				System.setProperty(key, blank);
				Assertions.assertTrue(PropertiesHelper.getBool(key, true));
				Assertions.assertFalse(PropertiesHelper.getBool(key, false));
			}
			// 合法字面量（忽略大小写）：结果与默认值无关
			for (var t : new String[]{"true", "TRUE", "True"})
				checkLiteral(key, t, true);
			for (var f : new String[]{"false", "FALSE", "False"})
				checkLiteral(key, f, false);
			// 非法值 → 默认值（修复前会被parseBoolean静默当成false）
			for (var bad : new String[]{"yes", "on", "0", "1", "true1", "fals"})
				checkInvalid(key, bad);
		} finally {
			System.clearProperty(key);
		}
	}

	private static void checkLiteral(String key, String value, boolean literal) {
		System.setProperty(key, value);
		Assertions.assertEquals(literal, PropertiesHelper.getBool(key, true), () -> "value=" + value + " def=true");
		Assertions.assertEquals(literal, PropertiesHelper.getBool(key, false), () -> "value=" + value + " def=false");
	}

	private static void checkInvalid(String key, String value) {
		System.setProperty(key, value);
		// def=true 必须返回true：证明走的是默认值而非parseBoolean的false
		Assertions.assertTrue(PropertiesHelper.getBool(key, true), () -> "value=" + value);
		Assertions.assertFalse(PropertiesHelper.getBool(key, false), () -> "value=" + value);
	}
}
