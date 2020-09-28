using System;
using System.Collections.Generic;
using System.Text;
using Game.Login;
using Zeze.Net;
using Zeze.Transaction;

namespace Game.Login
{
    public class Onlines
    {
        private tonline table;

        public Onlines(tonline table)
        {
            this.table = table;
        }

        public void Send(long roleId, Protocol p)
        {
            BOnline online = table.Get(roleId);
            if (null == online)
            {
                return;
            }

            AsyncSocket socket = Game.App.Instance.Server.GetSocket(online.SessionId);
            if (null == socket)
            {
                return;
            }

            p.Send(socket);
        }

        public void Send(IEnumerable<long> roleIds, Protocol p)
        {
            foreach (long roleId in roleIds)
                Send(roleId, p);
        }

        public void SendWhileCommit(long roleId, Protocol p)
        {
            Transaction.Current.RunWhileCommit(() => Send(roleId, p));
        }

        public void SendWhileCommit(IEnumerable<long> roleIds, Protocol p)
        {
            Transaction.Current.RunWhileCommit(() => Send(roleIds, p));
        }

        public void SendWhileRollback(long roleId, Protocol p)
        {
            Transaction.Current.RunWhileRollback(() => Send(roleId, p));
        }

        public void SendWhileRollback(IEnumerable<long> roleIds, Protocol p)
        {
            Transaction.Current.RunWhileRollback(() => Send(roleIds, p));
        }
    }
}
