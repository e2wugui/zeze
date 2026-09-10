using System;
using System.Collections.Generic;

namespace Zeze.Util
{
	public sealed class Ranges
	{
		public sealed class Range
		{
			private readonly int first; // [first, last)
			private readonly int last;

			public Range(string[] pair)
			{
				first = int.Parse(pair[0]);
				last = pair.Length > 1 ? int.Parse(pair[1]) + 1 : first + 1;
				if (first < 0 || last < 0 || first >= last)
					throw new Exception("error new range : " + this);
			}
			public Range(int first, int last)
			{
				this.first = first;
				this.last = last;
				if (first >= last)
					throw new Exception("error new range : " + this);
			}

			public bool HasIntersection(Range r)
			{
				return !(r.last <= first || r.first >= last);
			}

			public override string ToString()
			{
				if (first + 1 == last)
					return first.ToString();
				// 打印闭端点（last-1）与解析对称（解析按闭区间 +1 转半开），
				// 保证 parse(ToString(r)) == r，往返不漂移（FND3-17）。
				return first.ToString() + "-" + (last - 1).ToString();
			}

			public bool Include(Range r)
			{
				return r.first >= first && r.last <= last;
			}
		}

		private readonly List<Range> ranges = new List<Range>();

		public Ranges()
		{
		}

		public Ranges(string str)
		{
			foreach (string s in str.Split(','))
			{
				if (s.Length == 0)
					continue;
				string[] pair = s.Split('-');
				if (pair.Length == 0)
					continue;
				if (pair.Length > 2)
					throw new Exception("error format: '" + str + "'");
				ranges.Add(new Range(pair));
			}
		}

		public void CheckAdd(Ranges rs)
		{
			foreach (Range r in rs.ranges)
				CheckAdd(r);
		}

		public void CheckAdd(Range r)
		{
			foreach (Range _r in ranges)
            {
				if (_r.HasIntersection(r))
					throw new Exception(ranges + " checkAdd " + r);
			}
			ranges.Add(r);
		}

		public void CheckAdd(int type)
		{
			CheckAdd(new Range(type, type + 1));
		}

		public bool Include(Ranges rs)
		{
			foreach (Range r in rs.ranges)
            {
				if (Include(r))
					return true;
			}
			return false;
		}

		public bool Include(Range r)
		{
			foreach (Range _r in ranges)
            {
				if (_r.Include(r))
					return true;
			}
			return false;
		}

		public void AssertInclude(Range r)
		{
			if (Include(r))
				return;
			throw new Exception(ranges + " NOT Include " + r);
		}

		public void AssertInclude(Ranges rs)
		{
			foreach (Range r in rs.ranges)
				AssertInclude(r);
		}

		public void AssertInclude(int type)
		{
			AssertInclude(new Range(type, type + 1));
		}

		// 逗号分隔的闭区间串，与构造解析同构：parse(ToString(rs)) == rs（FND3-17），
		// 同时让 CheckAdd/AssertInclude 的冲突消息可读（原为 Object 默认的类型名）。
		public override string ToString()
		{
			var sb = new System.Text.StringBuilder();
			foreach (Range r in ranges)
			{
				if (sb.Length > 0)
					sb.Append(',');
				sb.Append(r);
			}
			return sb.ToString();
		}
	}
}
