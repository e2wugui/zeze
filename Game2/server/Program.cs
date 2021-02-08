using System;
using System.Threading;
using System.Collections.Generic;

using System.Runtime.Serialization.Formatters.Binary;
using System.IO;

namespace gsd
{
    class Program
    {
        static void Main(string[] args)
        {
            string srcDirWhenPostBuild = null;
            for (int i = 0; i < args.Length; ++i)
            {
                switch (args[i])
                {
                    case "-srcDirWhenPostBuild":
                        srcDirWhenPostBuild = args[++i];
                        break;
                }
            }
            if (null != srcDirWhenPostBuild)
            {
                Game.ModuleRedirect.Instance.SrcDirWhenPostBuild = srcDirWhenPostBuild;
                Game.App.Instance.Create();
                if (Game.ModuleRedirect.Instance.HasNewGen)
                {
                    Console.WriteLine("ModuleRedirect HasNewGen. Please Rebuild Now.");
                }
                Game.App.Instance.Destroy();
                return;
            }

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
