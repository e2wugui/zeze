using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Util
{
    public static class Time
    {
        public static long NowMillis => DateTimeOffset.Now.ToUnixTimeMilliseconds();
    }
}
