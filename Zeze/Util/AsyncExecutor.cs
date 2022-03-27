using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Util
{
    public class AsyncExecutor
    {
        private BlockingCollection<(TaskCompletionSource<bool>, Action)> Queue { get; } = new();
        public Util.AtomicInteger Pooling { get; } = new();
        public Func<int> MaxPoolSize { get; }

        public AsyncExecutor(Func<int> maxPoolSize)
        {
            MaxPoolSize = maxPoolSize;
        }

        public void Execute(TaskCompletionSource<bool> source, Action action)
        {
            var pending = Pooling.IncrementAndGet();
            if (pending >= MaxPoolSize())
            {
                Pooling.AddAndGet(-1); // rollback
                Queue.Add((source, action));
            }
            else
            {
                Task.Run(() =>
                {
                    try
                    {
                        action();
                    }
                    catch (Exception ex)
                    {
                        source.TrySetException(ex);
                    }
                    finally
                    {
                        Pooling.AddAndGet(-1); // done
                        if (Queue.TryTake(out var item))
                            Execute(item.Item1, item.Item2);
                    }
                });
            }
        }
    }
}
