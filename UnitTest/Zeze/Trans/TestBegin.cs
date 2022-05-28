using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Util;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestBegin
    {
        public class MyBean : Bean
        {
            public override void Decode(ByteBuffer bb)
            {
                throw new NotImplementedException();
            }

            public override void Encode(ByteBuffer bb)
            {
                throw new NotImplementedException();
            }

            protected override void InitChildrenRootInfo(Record.RootInfo root)
            {
            }

            public int _i;

            class MyLog : Log<int>
            {
                public MyLog(MyBean bean, int value)
                {
                    Belong = bean;
                    Value = value;
                    VariableId = 0;
                }

                public override void Commit()
                {
                    ((MyBean)Belong)._i = Value;
                }
            }
            public int I
            {
                get
                {
                    MyLog log = (MyLog)Transaction.Current.GetLog(this.ObjectId + 0);
                    return (null != log) ? log.Value : _i;
                }
                set
                {
                    Transaction.Current.PutLog(new MyLog(this, value));
                }
            }
        }

        /*
        [TestMethod]
        public async System.Threading.Tasks.Task TestAssert()
        {
            await Mission.CallAsync(() => { Assert.IsTrue(false); return System.Threading.Tasks.Task.FromResult(0L); }, "");
        }
        */


        private Locks Locks = new Locks();
        [TestMethod]
        public void TestRollback()
        {
            Transaction.Create(Locks);
            try
            {
                Transaction.Current.Begin();

                // process
                MyBean bean = new MyBean();
                Assert.AreEqual(bean.I, 0);

                bean.I = 1;
                Assert.AreEqual(bean.I, 1);

                Transaction.Current.Rollback();
                Assert.AreEqual(bean.I, 0);
            }
            finally
            {
                Transaction.Destroy();
            }
        }

        [TestMethod]
        public void TestCommit()
        {
            Transaction.Create(Locks);
            try
            {
                Transaction.Current.Begin();

                // process
                MyBean bean = new MyBean();
                Assert.AreEqual(bean.I, 0);

                bean.I = 1;
                Assert.AreEqual(bean.I, 1);

                Transaction.Current.Commit();
                Assert.AreEqual(bean.I, 1);
            }
            finally
            {
                Transaction.Destroy();
            }
        }

        private void ProcessNestRollback(MyBean bean)
        {
            Assert.AreEqual(bean.I, 1);
            Transaction.Current.Begin();
            Assert.AreEqual(bean.I, 1);
            bean.I = 2;
            Assert.AreEqual(bean.I, 2);
            Transaction.Current.Rollback();
            Assert.AreEqual(bean.I, 1);
        }

        [TestMethod]
        public void TestNestRollback()
        {
            Transaction.Create(Locks);
            try
            {
                Transaction.Current.Begin();

                // process
                MyBean bean = new MyBean();
                Assert.AreEqual(bean.I, 0);

                bean.I = 1;
                Assert.AreEqual(bean.I, 1);
                ProcessNestRollback(bean);
                Assert.AreEqual(bean.I, 1);

                Transaction.Current.Commit();
                Assert.AreEqual(bean.I, 1);
            }
            finally
            {
                Transaction.Destroy();
            }
        }

        private void ProcessNestCommit(MyBean bean)
        {
            Assert.AreEqual(bean.I, 1);
            Transaction.Current.Begin();
            Assert.AreEqual(bean.I, 1);
            bean.I = 2;
            Assert.AreEqual(bean.I, 2);
            Transaction.Current.Commit();
            Assert.AreEqual(bean.I, 2);
        }
 
        [TestMethod]
        public void TestNestCommit()
        {
            Transaction.Create(Locks);
            try
            {
                Transaction.Current.Begin();

                // process
                MyBean bean = new MyBean();
                Assert.AreEqual(bean.I, 0);

                bean.I = 1;
                Assert.AreEqual(bean.I, 1);
                ProcessNestCommit(bean);
                Assert.AreEqual(bean.I, 2);

                Transaction.Current.Commit();
                Assert.AreEqual(bean.I, 2);
            }
            finally
            {
                Transaction.Destroy();
            }
        }
    }
}
