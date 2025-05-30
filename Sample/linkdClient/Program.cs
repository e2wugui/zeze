using System;
using System.Threading;

namespace linkd
{
    public class Program
    {
        public static void Main(string[] args)
        {
            Zeze.Serialize.ByteBuffer.BinaryNoCopy = true;
            Zezex.App.Instance.Start(args);
            try
            {
                while (true)
                {
                    Thread.Sleep(1000);
                }
            }
            finally
            {
                Zezex.App.Instance.Stop();
            }
        }
    }
}
