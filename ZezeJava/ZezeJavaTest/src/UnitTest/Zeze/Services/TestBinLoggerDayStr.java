package UnitTest.Zeze.Services;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import Zeze.Services.BinLogger;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND3-42回归：日志文件名前缀必须是8位零填充日期数字（BinLoggerService字段注释
 * 明示的契约）。未修复：%4d%2d%2d宽度用空格右对齐补齐——2026-09-08得到
 * "2026 9 8"（含空格，仅6位数字），一年约108天外部按yyyyMMdd通配的工具失配。
 * 扫描一年跨度的日期戳，断言每个输出都是8位纯数字且与期望格式一致（红绿不受运行日期影响）。
 * 2026-09-21修订：FND4-75后实现改java.time（LocalDate.ofEpochDay直渲染），原反射的
 * BinLogger.timeZoneOffset字段已随重构删除（30轮压测28轮NoSuchFieldException确定性假红）。
 * 期望值改用UTC口径GregorianCalendar独立推算：dayStamp*86400_000L即UTC零点，
 * 不复用被测实现的时区逻辑，保持oracle独立性。
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
		var utc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		for (var i = 0; i <= 400; i++) {
			var dayStamp = today + i;
			var str = (String)toDayStr.invoke(null, dayStamp);
			utc.setTimeInMillis(dayStamp * 86400_000L); // epochDay*一天=UTC零点，与实现口径一致
			var expected = String.format("%04d%02d%02d",
					utc.get(Calendar.YEAR), utc.get(Calendar.MONTH) + 1, utc.get(Calendar.DAY_OF_MONTH));
			Assertions.assertEquals(expected, str, "dayStamp=" + dayStamp + " 必须是零填充8位数字（FND3-42）");
			Assertions.assertEquals(8, str.length(), "dayStamp=" + dayStamp);
			Assertions.assertFalse(str.chars().anyMatch(c -> !Character.isDigit(c)),
					"dayStamp=" + dayStamp + " 不得含空格等非数字字符");
		}
	}
}
