using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Util
{
    // 使用自己的hash算法，因为 TypeId 会持久化，不能因为算法改变导致值变化。
    // XXX: 这个算法定好之后，就不能变了。系统库的Hash算法可能改变。
    public class FixedHash
    {
        // This is a Knuth hash
        public static long Hash64(string name)
        {
            ulong hashedValue = 3074457345618258791ul;
            for (int i = 0; i < name.Length; i++)
            {
                hashedValue += name[i];
                hashedValue *= 3074457345618258799ul;
            }
            return (long)hashedValue;
        }

        public static int Hash32(string name)
        {
            ulong hash64 = (ulong)Hash64(name);
            uint hash32 = (uint)(hash64 & 0xffffffff) ^ (uint)(hash64 >> 32);
            return (int)hash32;
        }

        // calc_hashnr
        public static int calc_hashnr(long value)
        {
            return (int)((value * unchecked((long)0x9E3779B97F4A7C15L)) >> 32);
        }

        public static int calc_hashnr(string str)
        {
            int hash = 0;
            for (int i = 0, n = str.Length; i < n; i++)
                hash = (hash * 16777619) ^ str[i];
            return hash;
        }

        public static int calc_hashnr(byte[] keys)
        {
            return calc_hashnr(keys, 0, keys.Length);
        }

        public static int calc_hashnr(byte[] keys, int offset, int len)
        {
            int hash = 0;
            for (int end = offset + len; offset < end; offset++)
                hash = (hash * 16777619) ^ keys[offset];
            return hash;
        }


    }
}
