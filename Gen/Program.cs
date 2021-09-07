using System;

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

                case "ExportZezex":
                    Zeze.Util.Zezex.Main(args);
                    break;

                case "TikvTest":
                    Zeze.Tikv.Test.Run(args[0]);
                    break;

                case "RaftTest":
                    new Zeze.Raft.Test().Run();
                    break;
            }

        }
    }
}
