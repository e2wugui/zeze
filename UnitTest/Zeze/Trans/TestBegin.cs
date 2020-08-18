using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Serialize;
using Zeze.Transaction;

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

            protected override void InitChildrenTableKey(TableKey root)
            {
                throw new NotImplementedException();
            }

            public int _i;

            class MyLog : Log<MyBean, int>
            {
                public MyLog(MyBean bean, int value) : base(bean, value)
                {

                }

                public override long LogKey => Bean.ObjectId + 0;

                public override void Commit()
                {
                    ((MyBean)Bean)._i = Value;
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

        [TestMethod]
        public void TestRollback()
        {
            Transaction.Create();
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
            Transaction.Create();
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
            Transaction.Create();
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
            Transaction.Create();
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
