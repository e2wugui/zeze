using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Zeze.Util
{
    public class EventDispatcher
    {
        public string Name { get; }

        public EventDispatcher(string name)
        {
            this.Name = name;
        }

        public enum Mode
        {
            RunEmbed,
            RunProcedure,
            RunThread,
        }

        public class Events
        {
            private ConcurrentDictionary<long, Func<object, EventArgs, Task>> Handles = new();
            private Util.AtomicLong NextId = new();

            public class Canceler
            {
                public Events Events { get; }
                public long Id { get; }

                public Canceler(Events events, long id)
                {
                    Events = events;
                    Id = id;
                }

                public void Cancel()
                {
                    Events.Handles.Remove(Id, out _);
                }
            }

            public Canceler Add(Func<object, EventArgs, Task<long>> handle)
            {
                var next = NextId.IncrementAndGet();
                if (!Handles.TryAdd(next, handle))
                {
                    throw new Exception("Impossible!");
                }
                return new Canceler(this, next);
            }

            public ICollection<Func<object, EventArgs, Task>> values()
            {
                return this.Handles.Values;
            }
        }

        public Events RunEmbedEvents { get; } = new();
        public Events RunProcedureEvents { get; } = new();
        public Events RunThreadEvents { get; } = new();

        /**
         * 注册事件处理函数，
         * @return 如果需要取消注册，请保存返回值，并调用其cancel。
         */
        public Events.Canceler Add(Mode mode, Func<object, EventArgs, Task<long>> handle)
        {
            switch (mode)
            {
                case Mode.RunEmbed:
                    return RunEmbedEvents.Add(handle);

                case Mode.RunProcedure:
                    return RunProcedureEvents.Add(handle);

                case Mode.RunThread:
                    return RunThreadEvents.Add(handle);
            }
            throw new Exception($"Unknown mode={mode}");
        }

        // 事件派发。需要触发者在明确的地方显式的调用。

        // 启动新的线程执行。
        public void TriggerThread(object sender, EventArgs arg)
        {
            foreach (var handle in RunThreadEvents.values())
            {
                _ = Mission.CallAsync(async () =>
                {
                    await handle(sender, arg);
                    return 0;
                }, $"EventDispatch.{Name}.TriggerThread");
            }
        }

        private static readonly ILogger logger = LogManager.GetLogger(typeof(EventDispatcher));

        // 在当前线程中，创建新的存储过程并执行，忽略所有错误。
        public async Task TriggerProcedureIgnoreError(Application app, object sender, EventArgs arg)
        {
            foreach (var handle in RunProcedureEvents.values())
            {
                try
                {
                    await app.NewProcedure(async () =>
                    {
                        await handle(sender, arg);
                        return 0;
                    }, $"EventDispatch.{Name}.TriggerProcedureIgnoreError").CallAsync();
                    // return error has log inner.
                }
                catch (Exception ex)
                {
                    logger.Error(ex);
                }
            }
        }

        // 嵌入当前线程执行，所有错误都报告出去，如果需要对错误进行特别处理，需要自己遍历Handles手动触发。
        public async Task TriggerEmbed(object sender, EventArgs arg)
        {
            foreach (var handle in RunEmbedEvents.values())
            {
                await handle(sender, arg);
            }
        }

        // 在当前线程中，创建新的存储过程并执行，所有错误都报告出去，如果需要对错误进行特别处理，需要自己遍历Handles手动触发。
        public async Task TriggerProcedure(Application app, object sender, EventArgs arg)
        {
            foreach (var handle in RunProcedureEvents.values())
            {
                var rc = await app.NewProcedure(async () =>
                {
                    await handle(sender, arg);
                    return 0L;
                }, $"EventDispatch.{Name}.TriggerProcedure").CallAsync();
                if (0L != rc)
                    throw new Exception($"Nest Call Fail. return={rc}");
            }
        }
    }
}
