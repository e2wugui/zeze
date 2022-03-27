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
        private BlockingCollection<(TaskCompletionSource<object>, Action)> Queue { get; } = new();
        public Util.AtomicInteger Pooling { get; } = new();
        public Func<int> MaxPoolSize { get; }

        public AsyncExecutor(Func<int> maxPoolSize)
        {
            MaxPoolSize = maxPoolSize;
        }

        public async Task RunAsync(Action action)
        {
            var source = new TaskCompletionSource<object>(TaskCreationOptions.RunContinuationsAsynchronously);
            Run(source, action);
            await source.Task;
        }

        public void Run(TaskCompletionSource<object> source, Action action)
        {
            var pending = Pooling.IncrementAndGet();
            if (pending >= MaxPoolSize())
            {
                Pooling.AddAndGet(-1); // rollback
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
                        Pooling.AddAndGet(-1); // done
                        if (Queue.TryTake(out var item))
                            Run(item.Item1, item.Item2);
                    }
                });
            }
        }
    }
}
