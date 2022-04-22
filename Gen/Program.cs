using System;
using System.Collections.Generic;
using System.Globalization;
using System.Threading.Tasks;

namespace Gen
{
    public class Program
    {
        public static void Main(string[] args)
        {
            string command = "gen";
            for (int i = 0; i < args.Length; ++i)
            {
                if (args[i].Equals("-c"))
                    command = args[++i];
            }
            switch (command)
            {
                case "gen":
                    Zeze.Gen.Program.Main(args);
                    break;

                case "genr":
                    Zeze.Builtin.RedirectGenMain.Main(args);
                    break;

                case "ExportZezex":
                    Zeze.Util.Zezex.Main(args);
                    break;
            }

        }
    }
}
