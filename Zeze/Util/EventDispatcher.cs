using System;
using System.Collections.Concurrent;
using System.Collections.Generic;

namespace Zeze.Util
{
    /// <summary>
    /// 这个类恐怕没什么用。写在这里主要是为了一个建议：
    /// 即事件应该在新的事务中执行。不要嵌套到触发者的事务中，否则可能无法控制。
    /// </summary>
    public class EventDispatcher
    {
        private ConcurrentDictionary<string, Func<object, EventArgs, System.Threading.Tasks.Task<long>>> Handles { get; } = new ();

        public void AddEventHandle(Func<object, EventArgs, System.Threading.Tasks.Task<long>> handle, string name = null)
        {
            if (null == name)
            {
                name = handle.Method.Name;
            }
            if (false == Handles.TryAdd(name, handle))
                throw new Exception($"Handle for '{name}' exist.");
        }

        public void RemoveEventHandle(Func<object, EventArgs, System.Threading.Tasks.Task<long>> handle, string name = null)
        {
            if (null == name)
            {
                name = handle.Method.Name;
            }
            Handles.TryRemove(KeyValuePair.Create(name, handle));
        }

        public void Dispatch(Application app, object sender, EventArgs args)
        {
            foreach (var e in Handles)
            {
                Task.Run(app.NewProcedure(() => e.Value(sender, args), e.Key));
            }
        }
    }
}
