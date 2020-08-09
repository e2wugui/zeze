using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Util
{
    public static class Time
    {
        public static long NowUnixMillis => DateTimeOffset.Now.ToUnixTimeMilliseconds();

        public static DateTime UnixMillisToDateTime(long unixMillis)
        {
            DateTime origin = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc);
            return origin.AddMilliseconds(unixMillis);
        }

        public static long DateTimeToUnixMillis(DateTime time)
        {
            DateTime origin = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc);
            TimeSpan diff = time.ToUniversalTime() - origin;
            return (long)diff.TotalMilliseconds;
        }
    }
}
