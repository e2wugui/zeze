using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Serialize;
using Zeze.Raft.RocksRaft;
using Zeze.Util;

namespace UnitTest.Zeze.Misc
{
    [TestClass]
    public class TestRaftConcurrentMaps
    {
        public class Value : Bean
        {
            public int Int { get; set; }

            public override void Decode(ByteBuffer bb)
            {
                Int = bb.ReadInt();
            }

            public override void Encode(ByteBuffer bb)
            {
                bb.WriteInt(Int);
            }

            protected override void InitChildrenRootInfo(Record.RootInfo root)
            {
            }

            public override void FollowerApply(Log log)
            {
                throw new NotImplementedException();
            }

            public override void LeaderApplyNoRecursive(Log log)
            {
                throw new NotImplementedException();
            }

            public override Bean CopyBean()
            {
                throw new NotImplementedException();
            }
        }

        [TestMethod]
        public async Task TestRocksDbColumn()
        {
            FileSystem.DeleteDirectory("127.0.0.1_9091");
            FileSystem.DeleteDirectory("127.0.0.1_9092");
            FileSystem.DeleteDirectory("127.0.0.1_9093");
            var raftConfig = global::Zeze.Raft.RaftConfig.Load();
            raftConfig.Nodes.Clear();
            var node1 = new global::Zeze.Raft.RaftConfig.Node("127.0.0.1", 9091);
            var node2 = new global::Zeze.Raft.RaftConfig.Node("127.0.0.1", 9092);
            var node3 = new global::Zeze.Raft.RaftConfig.Node("127.0.0.1", 9093);
            raftConfig.Nodes[node1.Name] = node1;
            raftConfig.Nodes[node2.Name] = node2;
            raftConfig.Nodes[node3.Name] = node3;
            var storage = new Rocks(node1.Name, RocksMode.Pessimism, raftConfig);

            // 数据修改相关测试已经移到 UnitTest/Zeze/RocksRaft/ 下。
            storage.RegisterTableTemplate<int, Value>("int2value");

            var (cpdir, term, index) = await storage.Checkpoint();
            try
            {
                Console.WriteLine($"Cpdir={cpdir} Term={term} Index={index}");
                Assert.IsTrue(storage.Backup(cpdir, "backup"));
                Assert.IsTrue(storage.Restore("backup"));
            }
            finally
            {
                Directory.Delete(cpdir, true);
            }
        }

    }
}
