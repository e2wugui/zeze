package UnitTest.Zeze.Services;

import java.lang.reflect.Method;

import Zeze.Services.BinLogger;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND3-42回归：日志文件名前缀必须是8位零填充日期数字（BinLoggerService字段注释
 * 明示的契约）。未修复：%4d%2d%2d宽度用空格右对齐补齐——2026-09-08得到
 * "2026 9 8"（含空格，仅6位数字），一年约108天外部按yyyyMMdd通配的工具失配。
 * 扫描一年跨度的日期戳，断言每个输出都是8位纯数字且与期望格式一致（红绿不受运行日期影响）。
 * 期望值改经BASIC_ISO_DATE独立路径还原：FND4-75把toDayStamp/toDayStr的口径改为
 * DST感知的LocalDate后移除了timeZoneOffset字段，原静态块反射该字段必然
 * NoSuchFieldException（基线即坏，与日期契约无关）。
 */
@Fast
public class TestBinLoggerDayStr {

	@Test
	public void testDayStrEightDigitsNoSpace() throws Exception {
		var serviceClass = BinLogger.BinLoggerService.class;
		var toDayStamp = serviceClass.getDeclaredMethod("toDayStamp", long.class);
		toDayStamp.setAccessible(true);
		var toDayStr = serviceClass.getDeclaredMethod("toDayStr", int.class);
		toDayStr.setAccessible(true);

		// 从今天起扫一年+40天：必然覆盖个位数月/日（未修复时产生空格）与双位数的对照。
		var today = (int)toDayStamp.invoke(null, System.currentTimeMillis());
		for (var i = 0; i <= 400; i++) {
			var dayStamp = today + i;
			var str = (String)toDayStr.invoke(null, dayStamp);
			var expected = java.time.format.DateTimeFormatter.BASIC_ISO_DATE
					.format(java.time.LocalDate.ofEpochDay(dayStamp));
			Assertions.assertEquals(expected, str, "dayStamp=" + dayStamp + " 必须是零填充8位数字（FND3-42）");
			Assertions.assertEquals(8, str.length(), "dayStamp=" + dayStamp);
			Assertions.assertFalse(str.chars().anyMatch(c -> !Character.isDigit(c)),
					"dayStamp=" + dayStamp + " 不得含空格等非数字字符");
		}
	}
}
