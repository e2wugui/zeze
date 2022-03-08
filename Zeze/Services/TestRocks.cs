
using System;
using Zeze.Util;
using Zeze.Raft.RocksRaft;
using System.Collections.Generic;
using Zeze.Component.TestRocks;

namespace Zeze.Services
{
    public class TestRocks : AbstractTestRocks, IDisposable
    {
        public Rocks Rocks1 { get; }
        public Rocks Rocks2 { get; }
        public Rocks Rocks3 { get; }

        public List<Rocks> RocksList { get; }

        public Rocks GetLeader(Rocks skipMe)
        {
            while (true)
            {
                foreach (var rock in RocksList)
                {
                    if (rock == skipMe)
                        continue;
                    if (rock.IsLeader)
                        return rock;
                }
                System.Threading.Thread.Sleep(1000);
            }
        }

        public TestRocks()
        {
            FileSystem.DeleteDirectory("127.0.0.1_6000");
            FileSystem.DeleteDirectory("127.0.0.1_6001");
            FileSystem.DeleteDirectory("127.0.0.1_6002");

            Rocks1 = new Rocks("127.0.0.1:6000");
            Rocks2 = new Rocks("127.0.0.1:6001");
            Rocks3 = new Rocks("127.0.0.1:6002");

            RocksList = new List<Rocks> { Rocks1, Rocks2, Rocks3 };

            foreach (var rocks in RocksList)
            {
                // TODO RegisterLog 需要生成实现。代码移到 RegisterRocksTables 中。

                rocks.RegisterLog<LogSet1<int>>();
                rocks.RegisterLog<LogSet1<Zeze.Component.GlobalCacheManagerWithRaft.GlobalTableKey>>();
                rocks.RegisterLog<LogMap1<int, int>>();
                rocks.RegisterLog<LogMap2<int, Value>>();

                RegisterRocksTables(rocks);
            }

            foreach (var rocks in RocksList)
            {
                rocks.Raft.Server.Start();
            }
        }

        public void Dispose()
        {
            foreach (var rocks in RocksList)
            {
                rocks.Dispose();
            }
            RocksList.Clear();
        }
    }
}
