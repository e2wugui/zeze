using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Text;
using System.Threading.Tasks;

namespace WorldDemo
{
    public class Program
    {
        public static void Main(string[] args)
        {
            var account = "1";
            for (int i = 0; i < args.Length; i++)
            {
                if (args[i].Equals("-u"))
                    account = args[++i];
            }

            var app = new Zeze.App();
            app.Start();

            try
            {
                app.Test(account).Wait();
            }
            finally
            {
                app.Stop();
            }
        }
    }
}
