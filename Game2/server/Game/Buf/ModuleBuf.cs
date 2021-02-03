
using Zeze.Transaction;
using System.Collections.Generic;

namespace Game.Buf
{
    public partial class ModuleBuf : AbstractModule
    {
        public void Start(Game.App app)
        {
            _tbufs.ChangeListenerMap.AddListener(tbufs.VAR_Bufs, new BufChangeListener("Game.Buf.Bufs"));
        }

        public void Stop(Game.App app)
        {
        }

        [ModuleRedirect()]
        public virtual void Redirect(ICollection<int> icollection)
        { 
        }

        [ModuleRedirect()]
        public virtual void RedirectTest0(KeyValuePair<int, int> pair)
        {

        }

        [ModuleRedirect()]
        public virtual void RedirectTest(int x, Stack<int> stack)
        {

        }

        public class MyClass
        {
            public int i { get; set; }

            public class MyClass2<T, T2>
            { 
                public T t { get; set; }
            }
        }

        [ModuleRedirect()]
        public virtual int RedirectTest2(int x, MyClass.MyClass2<int, long> m2, Game.Bag.BBag bean, MyClass o, Dictionary<int, Dictionary<int, object>> y)
        {
            return 0;
        }


        class BufChangeListener : Zeze.Transaction.ChangeListener
        {
            public string Name { get; }

            public BufChangeListener(string name)
            {
                Name = name;
            }
            void ChangeListener.OnChanged(object key, Bean value)
            {
                // 记录改变，通知全部。
                BBufs record = (BBufs)value;

                SChanged changed = new SChanged();
                changed.Argument.ChangeTag = BBufChanged.ChangeTagRecordChanged;
                changed.Argument.Replace.AddRange(record.Bufs);

                Game.App.Instance.Game_Login.Onlines.SendReliableNotify((long)key, Name, changed);
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

                Game.App.Instance.Game_Login.Onlines.SendReliableNotify((long)key, Name, changed);
            }

            void ChangeListener.OnRemoved(object key)
            {
                SChanged changed = new SChanged();
                changed.Argument.ChangeTag = BBufChanged.ChangeTagRecordIsRemoved;
                Game.App.Instance.Game_Login.Onlines.SendReliableNotify((long)key, Name, changed);
            }
        }

        // TODO 如果宠物什么的如果也有buf，看情况处理：统一存到一个表格中（使用BFighetId），或者分开存储。
        public Bufs GetBufs(long roleId)
        {
            return new Bufs(roleId, _tbufs.GetOrAdd(roleId));
        }
    }
}
