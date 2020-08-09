using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Util;

namespace UnitTest.Zeze.Util
{
    [TestClass]
    public class TestScheduler
    {
        int SchedulerRun1Count = 0;
        int SchedulerRun2Count = 0;
        int SchedulerRun3Count = 0;

        [TestMethod]
        public void Test1()
        {
            Scheduler s = new Scheduler();
            System.Threading.Thread.Sleep(100);
            s.schedule(SchedulerRun1, 100, 100);
            System.Threading.Thread.Sleep(100);
            s.schedule(SchedulerRun2, 100, -1);
            System.Threading.Thread.Sleep(100);
            s.schedule(SchedulerRun3, 100, 100);
            System.Threading.Thread.Sleep(300);
            s.StopAndJoin();
            Assert.IsTrue(SchedulerRun1Count > 3);
            Assert.IsTrue(SchedulerRun2Count == 1);
            Assert.IsTrue(SchedulerRun1Count > 2);
        }

        void SchedulerRun1()
        {
            //Console.WriteLine("SchedulerRun1 " + Time.NowMillis);
            SchedulerRun1Count++;
        }

        void SchedulerRun2()
        {
            //Console.WriteLine("SchedulerRun2 " + Time.NowMillis);
            SchedulerRun2Count++;
        }

        void SchedulerRun3()
        {
            //Console.WriteLine("SchedulerRun3 " + Time.NowMillis);
            SchedulerRun3Count++;
        }
    }
}
