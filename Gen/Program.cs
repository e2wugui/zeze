using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Gen
{
    public class Program
    {
        private static void DoNothing()
        {

        }

        public static void Main(string[] args)
        {
            /*
            var b = new Zeze.Util.Benchmark();
            var tasks = new Task[1000_0000];
            for (int i = 0; i < tasks.Length; ++i)
                tasks[i] = new Task(DoNothing);
            b.Report("Create", tasks.Length);
            foreach (var task in tasks)
                task.Start();
            b.Report("Queue", tasks.Length);
            Task.WaitAll(tasks);
            b.Report("Done", tasks.Length);
            */
            //*
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
                    new Zeze.Raft.Test().Run();
                    break;
            }

        }
    }
}
