
using Zeze.Transaction;

namespace Game.Buf
{
    public sealed partial class Module : AbstractModule
    {
        public void Start(Game.App app)
        {
            _tbufs.ChangeListenerMap.AddListener(tbufs.VAR_Bufs, new BufChangeListener());
        }

        public void Stop(Game.App app)
        {
        }

        class BufChangeListener : Zeze.Transaction.ChangeListener
        {
            void ChangeListener.OnChanged(object key, Bean value)
            {
                // 记录改变，通知全部。
                BBufs record = (BBufs)value;

                SChanged changed = new SChanged();
                changed.Argument.ChangeTag = BBufChanged.ChangeTagRecordChanged;
                changed.Argument.Replace.AddRange(record.Bufs);

                Game.App.Instance.Game_Login_Module.Onlines.Send((long)key, changed);
            }

            void ChangeListener.OnChanged(object key, Bean value, ChangeNote note)
            {
                // 增量变化，通知变更。
                ChangeNoteMap2<int, BBuf> notemap2 = (ChangeNoteMap2<int, BBuf>)note;
                BBufs record = (BBufs)value;
                notemap2.MergeChangedToReplaced(record.Bufs);

                SChanged changed = new SChanged();
                changed.Argument.ChangeTag = BBufChanged.ChangeTagNormalChanged;

                changed.Argument.Replace.AddRange(notemap2.Replaced);
                foreach (var p in notemap2.Removed)
                    changed.Argument.Remove.Add(p);

                Game.App.Instance.Game_Login_Module.Onlines.Send((long)key, changed);
            }

            void ChangeListener.OnRemoved(object key)
            {
                SChanged changed = new SChanged();
                changed.Argument.ChangeTag = BBufChanged.ChangeTagRecordIsRemoved;
                Game.App.Instance.Game_Login_Module.Onlines.Send((long)key, changed);
            }
        }

        // TODO 如果宠物什么的如果也有buf，看情况处理：统一存到一个表格中（使用BFighetId），或者分开存储。
        public Bufs GetBufs(long roleId)
        {
            return new Bufs(roleId, _tbufs.GetOrAdd(roleId));
        }
    }
}
