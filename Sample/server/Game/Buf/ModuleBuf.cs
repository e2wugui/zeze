
using Zeze.Transaction;
using System.Collections.Generic;
using System.Threading.Tasks;
using Zeze.Transaction.Collections;

namespace Game.Buf
{
    public partial class ModuleBuf : AbstractModule
    {
        public void Start(Game.App app)
        {
            _tBufs.ChangeListenerMap.AddListener(new BufChangeListener("Game.Buf.Bufs"));
        }

        public void Stop(Game.App app)
        {
        }

        class BufChangeListener : ChangeListener
        {
            public string Name { get; }

            public BufChangeListener(string name)
            {
                Name = name;
            }

            public void OnChanged(object key, Changes.Record changes)
            {
                switch (changes.State)
                {
                    case Changes.Record.Remove:
                        {
                            var changed = new SChanged();
                            changed.Argument.ChangeTag = BBufChanged.ChangeTagRecordIsRemoved;
                            Game.App.Instance.ProviderImplementWithOnline.Online.SendReliableNotify((long)key, Name, changed);
                        }
                        break;

                    case Changes.Record.Put:
                        {
                            // 记录改变，通知全部。
                            var record = (BBufs)changes.Value;

                            var changed = new SChanged();
                            changed.Argument.ChangeTag = BBufChanged.ChangeTagRecordChanged;
                            changed.Argument.Replace.AddRange(record.Bufs);

                            Game.App.Instance.ProviderImplementWithOnline.Online.SendReliableNotify((long)key, Name, changed);
                        }
                        break;

                    case Changes.Record.Edit:
                        {
                            var logbean = changes.GetLogBean();
                            if (logbean.Variables.TryGetValue(tBufs.VAR_Bufs, out var note))
                            {
                                // 增量变化，通知变更。
                                var notemap2 = (LogMap2<int, BBuf>)note;
                                notemap2.MergeChangedToReplaced();

                                var changed = new SChanged();
                                changed.Argument.ChangeTag = BBufChanged.ChangeTagNormalChanged;

                                changed.Argument.Replace.AddRange(notemap2.Replaced);
                                foreach (var p in notemap2.Removed)
                                    changed.Argument.Remove.Add(p);

                                Game.App.Instance.ProviderImplementWithOnline.Online.SendReliableNotify((long)key, Name, changed);
                            }
                        }
                        break;
                }
            }
        }

        // 如果宠物什么的如果也有buf，看情况处理：
        // 统一存到一个表格中（使用BFighetId），或者分开存储。
        // 【建议分开处理】。
        public async Task<Bufs> GetBufs(long roleId)
        {
            return new Bufs(roleId, await _tBufs.GetOrAddAsync(roleId));
        }
    }
}
