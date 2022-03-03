using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Util
{
	public sealed class ComparerInt : IComparer<int>
	{
		public readonly static IComparer<int> Instance = new ComparerInt();

		public int Compare(int x, int y)
		{
			return x.CompareTo(y);
		}
	}

	public sealed class ComparerLong : IComparer<long>
	{
		public readonly static IComparer<long> Instance = new ComparerLong();

		public int Compare(long x, long y)
		{
			return x.CompareTo(y);
		}
	}

	public sealed class ComparerString : IComparer<string>
	{
		public readonly static IComparer<string> Instance = new ComparerString();

		public int Compare(string x, string y)
		{
			return x.CompareTo(y);
		}
	}
}
