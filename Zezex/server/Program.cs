using System;
using System.Threading;
using System.Collections.Generic;

using System.Runtime.Serialization.Formatters.Binary;
using System.IO;

namespace server
{
    class Program
    {
        static void Main(string[] args)
        {
            Game.App.Instance.Start(args);
            try
            {
                while (true)
                {
                    Thread.Sleep(1000);
                }
            }
            finally
            {
                Game.App.Instance.Stop();
            }
        }
    }
}
