using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace Zeze.Util
{
    public class AsyncExecutor
    {
        private BlockingCollection<(TaskCompletionSource<object>, Action)> Queue { get; } = new();
        public Util.AtomicInteger Running { get; } = new();
        public Func<int> MaxPoolSize { get; }

        public AsyncExecutor(Func<int> maxPoolSize)
        {
            MaxPoolSize = maxPoolSize;
        }

        public void Shutdown()
        {
            Queue.CompleteAdding();
            lock (this)
            {
                while (Running.Get() > 0)
                {
                    Monitor.Wait(this);
                }
            }
        }

        public async Task RunAsync(Action action)
        {
            var source = new TaskCompletionSource<object>(TaskCreationOptions.RunContinuationsAsynchronously);
            Run(source, action);
            await source.Task;
        }

        public void Run(TaskCompletionSource<object> source, Action action)
        {
            if (Queue.IsAddingCompleted)
                throw new InvalidOperationException("AsyncExecutor.Queue.IsAddingCompleted");

            var running = Running.IncrementAndGet();
            if (running >= MaxPoolSize())
            {
                Running.DecrementAndGet(); // rollback
                Queue.Add((source, action));
            }
            else
            {
                _ = Task.Run(() =>
                {
                    try
                    {
                        action();
                        source.TrySetResult(null);
                    }
                    catch (Exception ex)
                    {
                        source.TrySetException(ex);
                    }
                    finally
                    {
                        // done
                        TryRunNext();
                    }
                });
            }
        }

        private void TryRunNext()
        {
            var running = Running.DecrementAndGet();
            if (Queue.IsAddingCompleted)
            {
                // clear queue.
                while (Queue.TryTake(out var item))
                {
                    item.Item1.TrySetCanceled();
                }
                if (0 == running)
                {
                    lock (this)
                    {
                        Monitor.PulseAll(this);
                    }
                }
            }
            else if (Queue.TryTake(out var item))
            {
                Run(item.Item1, item.Item2);
            }
        }
    }
}
