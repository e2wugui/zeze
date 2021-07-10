using System;
using System.Collections.Generic;
using System.Text;
using System.Threading;
using System.Collections.Concurrent;

namespace Zeze.Util
{
    public sealed class SimpleThreadPool
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        private BlockingCollection<Action> taskQueue = new BlockingCollection<Action>();
        private List<Thread> workers = new List<Thread>();
        public string Name { get; }

        public bool QueueUserWorkItem(Action action)
        {
            if (false == taskQueue.IsAddingCompleted)
                taskQueue.Add(action);
            // skip task
            return true;
        }

        public void Shutdown()
        {
            taskQueue.CompleteAdding();

            lock (this)
            {
                while (taskQueue.IsCompleted == false)
                {
                    Monitor.Wait(this);
                }
            }
        }

        public SimpleThreadPool(int workerThreads, string poolName)
        {
            Name = poolName;

            for (int i = 0; i < workerThreads; ++i)
            {
                workers.Add(new Thread(MainRun)
                {
                    Name = $"{Name}.{i}",
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
                    if (taskQueue.IsCompleted)
                    {
                        lock (this)
                        {
                            Monitor.PulseAll(this);
                        }
                        break;
                    }
                    action = taskQueue.Take();
                    if (null == action)
                        break;
                    action();
                }
                catch (OperationCanceledException)
                {
                    // skip
                }
                catch (Exception ex)
                {
                    logger.Error(ex, "SimpleThreadPool.MainRun {0}", action);
                }
            }
        }
    }
}
