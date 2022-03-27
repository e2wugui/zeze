
using System;
using Zeze.Util;
using Zeze.Raft.RocksRaft;
using System.Collections.Generic;
using Zeze.Beans.TestRocks;
using System.Threading.Tasks;

namespace Zeze.Services
{
    public class TestRocks : AbstractTestRocks, IDisposable
    {
        public Rocks Rocks1 { get; private set; }
        public Rocks Rocks2 { get; private set; }
        public Rocks Rocks3 { get; private set; }

        public List<Rocks> RocksList { get; private set; }

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
        }

        public async Task<TestRocks> OpenAsync()
        {
            FileSystem.DeleteDirectory("127.0.0.1_6000");
            FileSystem.DeleteDirectory("127.0.0.1_6001");
            FileSystem.DeleteDirectory("127.0.0.1_6002");

            Rocks1 = await new Rocks().OpenAsync("127.0.0.1:6000");
            Rocks2 = await new Rocks().OpenAsync("127.0.0.1:6001");
            Rocks3 = await new Rocks().OpenAsync("127.0.0.1:6002");

            RocksList = new List<Rocks> { Rocks1, Rocks2, Rocks3 };

            foreach (var rocks in RocksList)
            {
                RegisterRocksTables(rocks);
            }

            foreach (var rocks in RocksList)
            {
                rocks.Raft.Server.Start();
            }
            return this;
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
