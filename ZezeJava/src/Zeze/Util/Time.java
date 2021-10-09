package Zeze.Util;

import Zeze.*;
import java.time.*;

public final class Time {
	public static long getNowUnixMillis() {
		return DateTimeOffset.Now.ToUnixTimeMilliseconds();
	}

	public static LocalDateTime UnixMillisToDateTime(long unixMillis) {
		LocalDateTime origin = LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc);
		return origin.AddMilliseconds(unixMillis);
	}

	public static long DateTimeToUnixMillis(LocalDateTime time) {
		LocalDateTime origin = LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc);
		TimeSpan diff = time.ToUniversalTime() - origin;
		return (long)diff.TotalMilliseconds;
	}

	public static int GetWeekOfYear(LocalDateTime time) {
		// Gets the Calendar instance associated with a CultureInfo.
		CultureInfo myCI = CultureInfo.CurrentCulture;
		// FirstDayOfWeek在中国是周一，所以程序需要配置CurrentCulture。
		Calendar myCal = myCI.Calendar;
		// Gets the DTFI properties required by GetWeekOfYear.
		CalendarWeekRule myCWR = myCI.DateTimeFormat.CalendarWeekRule;
		DayOfWeek myFirstDOW = myCI.DateTimeFormat.FirstDayOfWeek;
		return myCal.GetWeekOfYear(time, myCWR, myFirstDOW);
	}

	/** 
	 没有国际化，没有考虑南半球。
	 
	 @param time
	 @return 
	*/
	public static int GetSimpleChineseSeason(LocalDateTime time) {
		var month = time.getMonthValue();
		if (month < 3) {
			return 4; // 12,1,2
		}
		if (month < 6) {
			return 1; // 3,4,5
		}
		if (month < 9) {
			return 2; // 6,7,8
		}
		if (month < 12) {
			return 3; // 9,10,11
		}
		return 4; // 12,1,2
	}
}