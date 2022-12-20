using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using demo.Module1;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Util;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestProcedure
    {
        readonly TestBegin.MyBean bean = new();

        public Task<long> ProcTrue()
        {
            bean.I = 123;
            Assert.AreEqual(bean.I, 123);
            return Task.FromResult(ResultCode.Success);
        }

        public Task<long> ProcFalse()
        {
            bean.I = 456;
            Assert.AreEqual(bean.I, 456);
            return Task.FromResult(ResultCode.Unknown);
        }

        public async Task<long> ProcNest()
        {
            Assert.AreEqual(bean.I, 0);
            bean.I = 1;
            Assert.AreEqual(bean.I, 1);
            {
                var r = await demo.App.Instance.Zeze.NewProcedure(ProcFalse, "ProcFalse").CallAsync();
                Assert.IsTrue(r != ResultCode.Success);
                Assert.AreEqual(bean.I, 1);
            }

            {
                var r = await demo.App.Instance.Zeze.NewProcedure(ProcTrue, "ProcFalse").CallAsync();
                Assert.IsTrue(r == ResultCode.Success);
                Assert.AreEqual(bean.I, 123);
            }

            return ResultCode.Success;
        }

        [TestInitialize]
        public void TestInit()
        {
            demo.App.Instance.Start();
        }

        [TestCleanup]
        public void TestCleanup()
        {
            demo.App.Instance.Stop();
        }

        [TestMethod]
        public void Test1()
        {
            var root = new TableKey(1, 1);
            // 特殊测试，拼凑一个record用来提供需要的信息。
            var table = new MyTable();
            var r = new Record<long, TestBegin.MyBean>(table, 1, bean);
            bean.InitRootInfo(r.CreateRootInfoIfNeed(root), null);
            var rc = demo.App.Instance.Zeze.NewProcedure(ProcNest, "ProcNest").CallSynchronously();
            Assert.IsTrue(rc == ResultCode.Success);
            // 最后一个 Call，事务外，bean 已经没法访问事务支持的属性了。直接访问内部变量。
            Assert.AreEqual(bean._i, 123);
        }

        public class MyTable : Table<long, TestBegin.MyBean>
        {
            public MyTable() : base("MyTable_1232")
            {
            }

            public override long DecodeKey(ByteBuffer bb)
            {
                return bb.ReadLong();
            }

            public override ByteBuffer EncodeKey(long key)
            {
                var bb = ByteBuffer.Allocate(16);
                bb.WriteLong(key);
                return bb;
            }
        }

        [TestMethod]
        public void TestNestLogOneLogDynamic()
        {
            Assert.IsTrue(0 == demo.App.Instance.Zeze.NewProcedure(async () =>
            {
                var value = await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(18989L);
                value.Bean12 = new Simple();
                value.Dynamic14.Bean = new Simple();
                value.Set10.Add(1);
                value.Map15.TryAdd(1L, 1L);
                value.List9.Add(new demo.Bean1());
                Assert.IsTrue(0 == demo.App.Instance.Zeze.NewProcedure(async () =>
                {
                    var value2 = await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(18989L);
                    value2.Bean12 = new Simple();
                    value2.Dynamic14.Bean = new Simple();
                    value2.Set10.Add(1);
                    value2.Map15.TryAdd(1L, 1L);
                    value2.List9.Add(new demo.Bean1());
                    return 0;
                }, "Nest").CallSynchronously());
                return 0;
            }, "testNestLogOneLogDynamic").CallSynchronously());
	}

    }
}
