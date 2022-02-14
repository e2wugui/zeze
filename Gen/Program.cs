using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Gen
{
    public class Program
    {
        public static void Main(string[] args)
        {
            //new UnitTest.Zeze.Net.TestSocketBeginXXX().Test();
            //return;
            /*
            new UnitTest.Zeze.Trans.TestGlobal().Test2App();
            return;
            new Benchmark.ABasicSimpleAddOneThread().testBenchmark();
            new Benchmark.BBasicSimpleAddConcurrentWithConflict().testBenchmark();
            new Benchmark.CBasicSimpleAddConcurrent().testBenchmark();
            return;
            // */
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
                case "RaftDump":
                    new Zeze.Raft.Test().Run(command, args);
                    break;
            }

        }
    }
}
