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
            var listStr = new List<string>();
            foreach (var e in c)
                listStr.Add(e.ToString());
            listStr.Sort(); // 排序，便于测试比较。

            sb.Append('[');
            int i = sb.Length;
            foreach (var e in listStr)
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
                var sortDict = new SortedDictionary<TK, TV>(comparer);
                foreach (var kv in dic)
                    sortDict.Add(kv.Key, kv.Value);
                dic = sortDict;
            }

            sb.Append('{');
            if (dic.Count == 0)
                sb.Append('}');
            else
            {
                foreach (var e in dic)
                    sb.Append(e.Key).Append(':').Append(e.Value).Append(',');
                sb[sb.Length - 1] = '}';
            }
        }

        // 十六进制字符串转成二进制数组. 大小写的A~F都支持,忽略其它字符
        public static void toBytes(string hex, byte[] bytes, int offset)
        {
            const long MASK = ~0x007E_0000_007E_03FFL; // 0~9;A~F;a~f
            for (int i = 0, v = 1, s = hex.Length; i < s; i++)
            {
                int c = hex[i] - '0';
                if ((c & ~63 | (int)(MASK >> c) & 1) == 0)
                {
                    v = (v << 4) + (c & 0xf) + ((c >> 4) & 1) * 9;
                    if (v > 0xff)
                    {
                        // ReSharper disable once IntVariableOverflowInUncheckedContext
                        bytes[offset++] = (byte)v;
                        v = 1;
                    }
                }
            }
        }

        // 十六进制字符串转成二进制数组. 大小写的A~F都支持,忽略其它字符
        public static byte[] toBytes(string hex)
        {
            const long MASK = 0x007E_0000_007E_03FFL; // 0~9;A~F;a~f
            int n = 0;
            for (int i = 0, s = hex.Length; i < s; i++)
            {
                int c = hex[i] - '0';
                if ((c & ~63) == 0)
                    n += (int)(MASK >> c) & 1;
            }
            byte[] b = new byte[n >> 1];
            toBytes(hex, b, 0);
            return b;
        }
    }
}
