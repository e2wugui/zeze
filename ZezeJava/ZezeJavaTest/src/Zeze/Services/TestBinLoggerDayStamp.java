package Zeze.Services;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * FND4-75：BinLogger天边界两口径不一致——toDayStamp用getRawOffset（不含夏令时），toDayStr经
 * ZoneId.systemDefault()（DST感知）。DST时区切换日附近天边界错开一天：轮转文件名与数据实际
 * 归属日期不符。修复后两口径同源（DST感知LocalDate）。中国时区不受影响，本测试切到
 * America/New_York构造DST窗口。经反射调用（修复前方法为private，反射兼容新旧两态）。
 * TimeZone.setDefault是进程级全局变更：@Isolated独占执行，防类级并行下污染并发的
 * 日期敏感测试（TestBinLoggerRotate/WriteFail/StopDiscard族实测被污染产生偶发红）。
 */
@Isolated
@Fast
public class TestBinLoggerDayStamp {

	private static int toDayStamp(long utcMs) throws Exception {
		Method m = BinLogger.BinLoggerService.class.getDeclaredMethod("toDayStamp", long.class);
		m.setAccessible(true);
		return (int)m.invoke(null, utcMs);
	}

	private static String toDayStr(int dayStamp) throws Exception {
		Method m = BinLogger.BinLoggerService.class.getDeclaredMethod("toDayStr", int.class);
		m.setAccessible(true);
		return (String)m.invoke(null, dayStamp);
	}

	@Test
	public void testDayStampDstAware() throws Exception {
		var saved = TimeZone.getDefault();
		try {
			TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
			var zone = ZoneId.of("America/New_York");

			// EDT（UTC-4）期内的本地 2026-07-01 00:30 = 04:30Z。
			// 旧实现按raw偏移（UTC-5）折算为本地 06-30 23:30 → 天戳错成前一天。
			var utcMs = ZonedDateTime.of(2026, 7, 1, 0, 30, 0, 0, zone).toInstant().toEpochMilli();
			var expectDay = (int)Instant.ofEpochMilli(utcMs).atZone(zone).toLocalDate().toEpochDay();
			Assertions.assertEquals(expectDay, toDayStamp(utcMs),
					"DST期间的天边界必须与本地日期一致（原raw偏移错一天）");
			Assertions.assertEquals("20260701", toDayStr(toDayStamp(utcMs)), "文件名渲染与天戳互逆");

			// 冬季（EST，raw=DST偏移）不受影响：口径一致
			var winterMs = ZonedDateTime.of(2026, 1, 15, 12, 0, 0, 0, zone).toInstant().toEpochMilli();
			Assertions.assertEquals("20260115", toDayStr(toDayStamp(winterMs)));

			// 本地午夜边界：恰逢0点归属当天
			var midnightMs = ZonedDateTime.of(2026, 7, 1, 0, 0, 0, 0, zone).toInstant().toEpochMilli();
			Assertions.assertEquals("20260701", toDayStr(toDayStamp(midnightMs)));
		} finally {
			TimeZone.setDefault(saved);
		}
	}
}
