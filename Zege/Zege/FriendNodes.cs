using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Builtin.Collections.LinkedMap;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Util;

namespace Zege.Friend
{
    public class FriendNodes : ChangesTable
    {
        public ModuleFriend ModuleFriend { get; }
        public string LinkedMapNameEndsWith { get; }

        public GetFriendNode GetFriendNodePending { get; set; }

        // 1. 浏览仅支持往后，用不删除开头的节点。
        //    如果浏览了非常多的节点，在View回到头部以后，从尾部开始删除部分节点。
        // 2. 头部节点在添加好友或者活跃Item(BringToTop)非常多的情况下会发生添加。
        //    即，当ChangeLog发生Put时，总在头部添加。
        public List<BGetFriendNode> Nodes { get; } = new();

        public FriendNodes(ModuleFriend module, string name)
        {
            ModuleFriend = module;
            LinkedMapNameEndsWith = name;
        }

        // Change Log Apply: 实现 ChangesTable
        public object DecodeKey(ByteBuffer bb)
        {
            var key = new BLinkedMapNodeKey();
            key.Decode(bb);
            return key;
        }

        public ConfBean NewValueBean()
        {
            return new BLinkedMapNode();
        }

        public ConfBean Get(object key)
        {
            var tkey = (BLinkedMapNodeKey)key;
            var indexOf = IndexOf(tkey.NodeId);
            if (indexOf < 0)
                return null;
            return Nodes[indexOf];
        }

        public void Put(object key, ConfBean value)
        {
            var tkey = (BLinkedMapNodeKey)key;
            var tvalue = (BLinkedMapNode)value;

            var node = new BGetFriendNode();
            node.NodeKey = tkey;
            node.Node = tvalue;

            // 新节点加入：添加好友时，头部节点满了；或者活跃的好友数量超出了头部节点的容量
            var indexOf = -2;
            ModuleFriend.UpdateItemsSource(indexOf, this, node);
        }

        public void Remove(object key)
        {
            var tkey = (BLinkedMapNodeKey)key;
            var indexOf = IndexOf(tkey.NodeId);
            if (indexOf >= 0)
            {
                Nodes.RemoveAt(indexOf);
                ModuleFriend.OnRemoveNode(tkey);
            }
        }

        // 好友管理实现
        public int IndexOf(long nodeId)
        {
            for (int i = 0; i < Nodes.Count; ++i)
            {
                if (Nodes[i].NodeKey.NodeId == nodeId)
                    return i;
            }
            return -1;
        }

        internal GetFriendNode TryNewGetFriendNode(bool forward)
        {
            if (forward)
            {
                if (Nodes.Count > 0)
                {
                    var last = Nodes[^1];
                    if (last.Node.NextNodeId == 0)
                        return null; // 已经是最后一个节点了。
                    var rpc = new GetFriendNode();
                    rpc.Argument.NodeId = last.Node.NextNodeId;
                    return rpc;
                }
                else
                {
                    var rpc = new GetFriendNode();
                    rpc.Argument.NodeId = 0;
                    return rpc;
                }
            }

            if (Nodes.Count > 0)
            {
                var last = Nodes[0];
                if (last.Node.PrevNodeId == 0)
                    return null; // 已经是最后一个节点了。
                var rpc = new GetFriendNode();
                rpc.Argument.NodeId = last.Node.PrevNodeId;
                return rpc;
            }
            else
            {
                var rpc = new GetFriendNode();
                rpc.Argument.NodeId = 0;
                return rpc;
            }

        }

        internal void TryGetFriendNode(bool forward)
        {
            if (GetFriendNodePending != null)
                return; // done

            GetFriendNodePending = TryNewGetFriendNode(forward);
            GetFriendNodePending?.Send(ModuleFriend.App.ClientService.GetSocket(), ModuleFriend.ProcessGetFriendNodeResponse);
        }

        internal Task<long> OnGetFriendNodeResponse(BGetFriendNode get)
        {
            GetFriendNodePending = null;
            ModuleFriend.UpdateItemsSource(IndexOf(get.NodeKey.NodeId), this, get);
            return Task.FromResult(0L);
        }
    }
}
