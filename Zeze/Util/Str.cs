
using System.Collections.Generic;
using System.Text;

namespace Zeze.Util
{
    public static class Str
    {
        public const int INDENT_MAX = 128;
        static readonly string[] INDENTS = new string[INDENT_MAX];

        static Str()
        {
            for (int i = 0; i < INDENT_MAX; i++)
                INDENTS[i] = new string(' ', i);
        }

        public static string Indent(int n)
        {
            if (n <= 0)
                return "";
            if (n >= INDENT_MAX)
                n = INDENT_MAX - 1;
            return INDENTS[n];
        }

        public static void BuildString<T>(StringBuilder sb, IEnumerable<T> c)
        {
            var liststr = new List<string>();
            foreach (var e in c)
                liststr.Add(e.ToString());
            liststr.Sort(); // 排序，便于测试比较。

            sb.Append('[');
            int i = sb.Length;
            foreach (var e in liststr)
            {
                sb.Append(e).Append(',');
            }
            int j = sb.Length;
            if (i == j)
                sb.Append(']');
            else
                sb[j - 1] = ']';
        }

        public static void BuildString<TK, TV>(StringBuilder sb, IDictionary<TK, TV> dic, IComparer<TK> comparer = null)
        {
            if (comparer != null)
            {
                var srtdict = new SortedDictionary<TK, TV>(comparer);
                foreach (var kv in dic)
                    srtdict.Add(kv.Key, kv.Value);
                dic = srtdict;
            }

            sb.Append('{');
            if (dic.Count == 0)
                sb.Append('}');
            else
            {
                foreach (var e in dic)
                    sb.Append(e.Key).Append(':').Append(e.Value).Append(',');
                sb[^1] = '}';
            }
        }
    }
}
