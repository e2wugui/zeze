package UnitTest.Zeze.Game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import Zeze.Builtin.Game.Rank.BConcurrentKey;
import Zeze.Game.Rank;
import harness.Fast;

/**
 * Rank 时间键确定性回归（评审 28a4b5fe 残留 P2/P3）。
 * newRankKey 原先依赖 Calendar.getInstance() 的 JVM 默认 locale 决定周定义
 * （firstDayOfWeek/minimalDaysInFirstWeek）：zh_CN（周一起周）与 en_US（周日起周）
 * 对同一毫秒算出不同周键，多服务器 locale 不一致/迁移/改默认 locale 时，
 * 同一真实周被劈成两键。现已在 newRankKey 中显式钉死周一为一周之始、最少 1 天
 * （与 zh_CN 生产行为一致）。时区仍取服务器默认：多时区部署需统一各服务器时区，
 * 本测试与 newRankKey 同用系统默认时区构造与判定。
 * newRankKey 为 public static，直接静态调用；纯键计算，无 app/外部依赖，标 @Fast。
 */
@Fast
public class TestRankWeekKey {
	private static final int RANK_TYPE = 1;

	/** 固定时刻（当日 12:00，系统默认时区）→ 毫秒；与 newRankKey 同一时区，来回换算落在同一日历日。 */
	private static long millis(int year, int month, int day) {
		return LocalDateTime.of(year, month, day, 12, 0, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	private static BConcurrentKey weekKey(int year, int month, int day) {
		return Rank.newRankKey(millis(year, month, day), RANK_TYPE, BConcurrentKey.TimeTypeWeek);
	}

	private static BConcurrentKey seasonKey(int year, int month, int day) {
		return Rank.newRankKey(millis(year, month, day), RANK_TYPE, BConcurrentKey.TimeTypeSeason);
	}

	private static void assertWeekKey(int year, int month, int day, int expectedWeekYear, long expectedWeek) {
		var k = weekKey(year, month, day);
		assertEquals(expectedWeekYear, k.getYear(), () -> year + "-" + month + "-" + day + " 基于周的年");
		assertEquals(expectedWeek, k.getOffset(), () -> year + "-" + month + "-" + day + " 周序");
	}

	private static void assertSeasonKey(int year, int month, int day, int expectedYear, long expectedSeason) {
		var k = seasonKey(year, month, day);
		assertEquals(expectedYear, k.getYear(), () -> year + "-" + month + "-" + day + " 季年");
		assertEquals(expectedSeason, k.getOffset(), () -> year + "-" + month + "-" + day + " 季序");
	}

	/**
	 * 跨年整周一键：2026-12-28(周一)..2027-01-03(周日) 同属 (2027, week1)；
	 * 2027-01-04(下一个周一) 进入 week2，与上一周不同键。
	 */
	@Test
	public void testCrossYearWholeWeekOneKey() {
		assertWeekKey(2026, 12, 28, 2027, 1);
		assertWeekKey(2026, 12, 29, 2027, 1);
		assertWeekKey(2026, 12, 30, 2027, 1);
		assertWeekKey(2026, 12, 31, 2027, 1);
		assertWeekKey(2027, 1, 1, 2027, 1);
		assertWeekKey(2027, 1, 2, 2027, 1);
		assertWeekKey(2027, 1, 3, 2027, 1);
		// 整周首尾两天的完整 BConcurrentKey 相等（同键）
		assertEquals(weekKey(2026, 12, 28), weekKey(2027, 1, 3));
		// 次周一换键
		assertWeekKey(2027, 1, 4, 2027, 2);
		assertNotEquals(weekKey(2027, 1, 3), weekKey(2027, 1, 4));
	}

	/**
	 * 同周同键/异周异键：2025-12-29(周一)..2026-01-04(周日) 同属 (2026, week1)；
	 * 次日(周一 2026-01-05) 属 week2。另补一组跨年周 2024-12-30..2025-01-05 → (2025, week1)。
	 */
	@Test
	public void testSameWeekSameKey() {
		var first = weekKey(2025, 12, 29);
		assertWeekKey(2025, 12, 29, 2026, 1);
		int[][] sameWeek = {{2025, 12, 30}, {2025, 12, 31}, {2026, 1, 1}, {2026, 1, 2}, {2026, 1, 3}, {2026, 1, 4}};
		for (var d : sameWeek)
			assertEquals(first, weekKey(d[0], d[1], d[2]), () -> d[0] + "-" + d[1] + "-" + d[2] + " 应与同周其他天同键");
		// 周日(01-04)与次周一(01-05)分属 week1/week2
		assertWeekKey(2026, 1, 5, 2026, 2);
		assertNotEquals(first, weekKey(2026, 1, 5));
		// 另一组跨年周：2024-12-30(周一)..2025-01-05(周日) → (2025, week1)
		assertWeekKey(2024, 12, 30, 2025, 1);
		assertWeekKey(2025, 1, 4, 2025, 1);
		assertWeekKey(2025, 1, 5, 2025, 1);
	}

	/**
	 * 季榜：一冬一键（12 月锚定次年）、春冬不同键。
	 * 2025-12-15 与 2026-01-15 同属一个冬季 → 同键 (2026, 季4)；
	 * 2026-03-15 是春季 → (2026, 季1)，不得并入冬季键；
	 * 2026-12-15 属下一个冬季 → 锚定 (2027, 季4)，与 2026 年 1 月的冬键不同。
	 */
	@Test
	public void testSeasonKeys() {
		assertSeasonKey(2025, 12, 15, 2026, 4);
		assertSeasonKey(2026, 1, 15, 2026, 4);
		assertEquals(seasonKey(2025, 12, 15), seasonKey(2026, 1, 15), "同一冬季(2025年12月-2026年2月)必须一键");
		assertSeasonKey(2026, 3, 15, 2026, 1);
		assertNotEquals(seasonKey(2026, 1, 15), seasonKey(2026, 3, 15), "春季不得与冬季同键");
		assertSeasonKey(2026, 12, 15, 2027, 4);
		assertNotEquals(seasonKey(2026, 1, 15), seasonKey(2026, 12, 15), "2026年12月与2026年1月是不同冬季，不得同键");
	}

	/**
	 * locale 无关性：同一时刻在 Locale.CHINA 与 Locale.US 下算出的周键必须相同
	 * （未钉死时 2026-12-27[周日] 在 zh_CN=(2026,week52)、en_US=(2027,week1)）。
	 * finally 恢复原 locale，避免污染其他测试。
	 */
	@Test
	public void testLocaleIndependent() {
		var orig = Locale.getDefault();
		try {
			int[][] days = {
					{2026, 12, 26}, {2026, 12, 27}, {2026, 12, 28}, {2026, 12, 31},
					{2027, 1, 1}, {2027, 1, 3}, {2027, 1, 4},
			};
			var keysChina = new BConcurrentKey[days.length];
			var keysUS = new BConcurrentKey[days.length];
			Locale.setDefault(Locale.CHINA);
			for (int i = 0; i < days.length; ++i)
				keysChina[i] = weekKey(days[i][0], days[i][1], days[i][2]);
			Locale.setDefault(Locale.US);
			for (int i = 0; i < days.length; ++i)
				keysUS[i] = weekKey(days[i][0], days[i][1], days[i][2]);
			for (int i = 0; i < days.length; ++i)
				assertEquals(keysChina[i], keysUS[i],
						days[i][0] + "-" + days[i][1] + "-" + days[i][2] + " 的周键不得随 locale 变化");
		} finally {
			Locale.setDefault(orig);
		}
	}
}
