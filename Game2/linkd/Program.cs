using System;
using System.Threading;

namespace linkd
{
    public class Program
    {
        public static void Main(string[] args)
        {
            gnet.App.Instance.Start();
            try
            {
                while (true)
                {
                    Thread.Sleep(1000);
                }
            }
            finally
            {
                gnet.App.Instance.Stop();
            }
        }
    }
}
