using System;
using System.Collections.Generic;
using System.Text;
using System.Threading;
using System.Collections.Concurrent;

namespace Zeze.Util
{
    public class SimpleThreadPool
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        private BlockingCollection<Action> taskQueue = new BlockingCollection<Action>();
        private List<Thread> workers = new List<Thread>();

        public bool QueueUserWorkItem(Action action)
        {
            taskQueue.Add(action);
            return true;
        }

        public SimpleThreadPool(int workerThreads)
        {
            for (int i = 0; i < workerThreads; ++i)
            {
                workers.Add(new Thread(MainRun)
                {
                    Name = "SimpleThreadPool.Worker." + i,
                    IsBackground = true
                });
            }

            foreach (Thread thread in workers)
            {
                thread.Start();
            }

        }

        private void MainRun()
        {
            while (true)
            {
                Action action = null;
                try
                {
                    action = taskQueue.Take();
                    if (null == action)
                        break;
                    action();
                }
                catch (Exception ex)
                {
                    logger.Error(ex, "SimpleThreadPool.MainRun {0}", action);
                }
            }
        }
    }
}
