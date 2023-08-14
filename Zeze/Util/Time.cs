using System;
using System.Diagnostics;
using System.Globalization;

namespace Zeze.Util
{
    public static class Time
    {
        public static long NowUnixMillis => DateTimeOffset.Now.ToUnixTimeMilliseconds();

        public static DateTime UnixMillisToDateTime(long unixMillis)
        {
            var origin = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc);
            return origin.AddMilliseconds(unixMillis);
        }

        public static long NanoTime()
        {
            double timestamp = Stopwatch.GetTimestamp();
            double nanoseconds = 1_000_000_000.0 * timestamp / Stopwatch.Frequency;
            return (long)nanoseconds;
        }

        public static long DateTimeToUnixMillis(DateTime time)
        {
            var origin = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc);
            TimeSpan diff = time.ToUniversalTime() - origin;
            return (long)diff.TotalMilliseconds;
        }

        public static int GetWeekOfYear(DateTime time)
        {
            // Gets the Calendar instance associated with a CultureInfo.
            var myCI = CultureInfo.CurrentCulture;
            // FirstDayOfWeek在中国是周一，所以程序需要配置CurrentCulture。
            Calendar myCal = myCI.Calendar;
            // Gets the DTFI properties required by GetWeekOfYear.
            CalendarWeekRule myCWR = myCI.DateTimeFormat.CalendarWeekRule;
            DayOfWeek myFirstDOW = myCI.DateTimeFormat.FirstDayOfWeek;
            return myCal.GetWeekOfYear(time, myCWR, myFirstDOW);
        }

        /// <summary>
        /// 没有国际化，没有考虑南半球。
        /// </summary>
        /// <param name="time"></param>
        /// <returns></returns>
        public static int GetSimpleChineseSeason(DateTime time)
        {
            var month = time.Month;
            if (month < 3) return 4; // 12,1,2
            if (month < 6) return 1; // 3,4,5
            if (month < 9) return 2; // 6,7,8
            if (month < 12) return 3; // 9,10,11
            return 4; // 12,1,2
        }
    }
}
