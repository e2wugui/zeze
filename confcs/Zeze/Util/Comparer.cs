using System.Collections.Generic;

namespace Zeze.Util
{
    public sealed class ComparerInt : IComparer<int>
    {
        public static readonly IComparer<int> Instance = new ComparerInt();

        public int Compare(int x, int y)
        {
            return x.CompareTo(y);
        }
    }

    public sealed class ComparerLong : IComparer<long>
    {
        public static readonly IComparer<long> Instance = new ComparerLong();

        public int Compare(long x, long y)
        {
            return x.CompareTo(y);
        }
    }

    public sealed class ComparerString : IComparer<string>
    {
        public static readonly IComparer<string> Instance = new ComparerString();

        public int Compare(string x, string y)
        {
            return string.CompareOrdinal(x, y);
        }
    }
}
