// See https://aka.ms/new-console-template for more information

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

string command = "";
for (int i = 0; i < args.Length; ++i)
{
    if (args[i].Equals("-c"))
        command = args[++i];
}

switch (command)
{
    case "TikvTest":
        Zeze.Tikv.Test.Run(args[0]);
        break;

    case "RaftTest":
    case "RaftDump":
        new Zeze.Raft.Test().Run(command, args);
        break;

    case "RocksRaft":
        new Zeze.Raft.RocksRaft.Test().Test_1();
        break;
}
