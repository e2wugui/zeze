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
            System.Threading.Thread.Sleep(100);
            Scheduler.Schedule(SchedulerRun1, 100, 100);
            System.Threading.Thread.Sleep(100);
            Scheduler.Schedule(SchedulerRun2, 100, -1);
            System.Threading.Thread.Sleep(100);
            Scheduler.Schedule(SchedulerRun3, 100, 100);
            System.Threading.Thread.Sleep(300);
            System.Threading.Thread.Sleep(300);
            Assert.IsTrue(SchedulerRun1Count > 3);
            Assert.IsTrue(SchedulerRun2Count == 1);
            Assert.IsTrue(SchedulerRun1Count > 2);
            Assert.IsTrue(SchedulerRun3Count > 2);
        }

        void SchedulerRun1(SchedulerTask ThisTask)
        {
            //Console.WriteLine("SchedulerRun1 " + Time.NowUnixMillis);
            SchedulerRun1Count++;
        }

        void SchedulerRun2(SchedulerTask ThisTask)
        {
            //Console.WriteLine("SchedulerRun2 " + Time.NowUnixMillis);
            SchedulerRun2Count++;
        }

        void SchedulerRun3(SchedulerTask ThisTask)
        {
            //Console.WriteLine("SchedulerRun3 " + Time.NowUnixMillis);
            SchedulerRun3Count++;
        }
    }
}
