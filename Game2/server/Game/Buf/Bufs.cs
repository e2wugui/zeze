using System;
using System.Collections.Generic;
using System.Text;
using Game.Buf;
using Microsoft.VisualBasic.CompilerServices;
using MySqlX.XDevAPI.CRUD;

namespace Game.Buf
{
    public class Bufs
    {
        public long RoleId { get; }
        private BBufs bean;

        public Bufs(long roleId, BBufs bean)
        {
            this.RoleId = roleId;
            this.bean = bean;
        }

        public Buf GetBuf(int id)
        {
            if (bean.Bufs.TryGetValue(id, out var bBuf))
            {
                switch (bBuf.Extra.TypeId)
                {
                    case BBufExtra.TYPEID: return new BufExtra(bBuf, (BBufExtra)bBuf.Extra.Bean);
                    default:
                        throw new System.Exception("unknown extra");
                }
            }
            return null;
        }

        public void Detach(int id)
        {
            if (bean.Bufs.Remove(id))
            {
                // 因为没有取消Scheduler，所以可能发生删除不存在的buf。
                Game.App.Instance.Game_Fight.StartCalculateFighter(RoleId);
            }
        }

        public void Attach(int id)
        {
            // TODO config: create Buf by id.
            BBuf buf = new BBuf();
            buf.Id = id;
            buf.AttachTime = Zeze.Util.Time.NowUnixMillis;
            buf.ContinueTime = 3600 * 1000; // 1 hour
            buf.Extra_Game_Buf_BBufExtra = new BBufExtra();

            new BufExtra(buf, buf.Extra_Game_Buf_BBufExtra).Attach(this);
        }

        public void Attach(Buf buf)
        {
            // TODO config: conflict 等
            bean.Bufs[buf.Id] = buf.Bean;

            Zeze.Util.Scheduler.Instance.Schedule(() => Detach(buf.Id), buf.ContinueTime);
            Game.App.Instance.Game_Fight.StartCalculateFighter(RoleId);
        }

        public void CalculateFighter(Game.Fight.Fighter fighter)
        {
            foreach (var bufid in bean.Bufs.Keys)
            {
                GetBuf(bufid).CalculateFighter(fighter);
            }
        }
    }
}
