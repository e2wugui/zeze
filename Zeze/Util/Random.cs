using System.Collections.Generic;

namespace Zeze.Util
{
    public sealed class Random
    {
        public static System.Random Instance { get; } = new System.Random();

        public static IList<T> Shuffle<T>(IList<T> list)
        {
            for (int i = 1; i < list.Count; i++)
            {
                int pos = Instance.Next(i + 1);
                var x = list[i];
                list[i] = list[pos];
                list[pos] = x;
            }
            return list;
        }

        public static T[] Shuffle<T>(T[] list)
        {
            for (int i = 1; i < list.Length; i++)
            {
                int pos = Instance.Next(i + 1);
                var x = list[i];
                list[i] = list[pos];
                list[pos] = x;
            }
            return list;
        }
    }
}
