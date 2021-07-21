using System;
using System.Collections.Generic;
using System.Text;
using Game.Buf;

namespace Game.Buf
{
    public abstract class Buf
    {
        private BBuf bean;

        public int Id => bean.Id;
        public long AttachTime => bean.AttachTime;
        public long ContinueTime => bean.ContinueTime;

        public BBuf Bean => bean; // 一般不要直接使用bean，最好都通过包装方法操作。

        public Buf(BBuf bean)
        {
            this.bean = bean;
        }

        public abstract void CalculateFighter(Game.Fight.Fighter fighter);

        // 可以重载用来实现加入时的特殊操作。
        public virtual void Attach(Bufs bufs)
        {
            bufs.Attach(this);
        }
    }
}
