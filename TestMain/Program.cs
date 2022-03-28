
namespace TestMain;

public class Program
{
    public static void Main(string[] args)
    {
        string command = "";
        for (int i = 0; i < args.Length; ++i)
        {
            if (args[i].Equals("-c"))
                command = args[++i];
        }

        switch (command)
        {
            case "go":
                new UnitTest.Zeze.Trans.TestZero().Go();
                break;

            case "BenchOneThread":
                new Benchmark.ABasicSimpleAddOneThread().testBenchmark();
                break;

            case "BenchConflict":
                new Benchmark.BBasicSimpleAddConcurrentWithConflict().testBenchmark();
                break;

            case "BenchConcurrent":
                new Benchmark.CBasicSimpleAddConcurrent().testBenchmark();
                break;

            case "TestGlobal":
                new UnitTest.Zeze.Trans.TestGlobal().Test2App();
                break;

            case "TestSocketBeginXXX":
                new UnitTest.Zeze.Net.TestSocketBeginXXX().Test();
                break;

            case "TikvTest":
                Zeze.Tikv.Test.Run(args[0]);
                break;

            case "RaftTest":
            case "RaftDump":
                new Zeze.Raft.Test().Run(command, args).Wait();
                break;

            case "RocksRaft":
                new Zeze.Raft.RocksRaft.Test().Test_1().Wait();
                break;
        }
    }
}
