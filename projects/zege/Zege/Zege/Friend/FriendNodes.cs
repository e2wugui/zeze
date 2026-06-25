using NLog;
using System;
using Zeze.Builtin.Collections.LinkedMap;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Transaction.Collections;

namespace Zege.Friend
{
    public class FriendNodes
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public ModuleFriend ModuleFriend { get; }
        public string LinkedMapNameEndsWith { get; }

        public GetFriendNode GetFriendNodePending { get; set; }

        // 1. 浏览仅支持往后，不删除开头的节点。
        //    如果浏览了非常多的节点，在View回到头部以后，从尾部开始删除部分节点。
        // 2. 头部节点在添加好友或者活跃Item(BringToTop)非常多的情况下会发生添加。
        //    即，当ChangeLog发生Put时，总在头部添加。
        public List<BGetFriendNode> Nodes { get; } = new();

        // isFirst 比较不爽的参数！用来指示FriendNodes在ItemSource中的顺序。
        // 现在只有两个FriendNodes，用个bool简单表示一下。
        // 这个参数用于UpdateItemSource处理Insert时，当自己的Nodes是空的时候，
        // 没有这个参数Insert无法确定ItemSource中的Insert Index。
        // see UpdateItemSource
        public bool FirstItemSource { get; }
        public Action LoadAllCompleted { get; private set; }

        public FriendNodes(ModuleFriend module, string name, bool firstItemSource)
        {
            ModuleFriend = module;
            LinkedMapNameEndsWith = name;
            FirstItemSource = firstItemSource;
        }

        public void FollowerApply(BLinkedMapNodeKey key, ByteBuffer bb)
        {
            int state;
            state = bb.ReadInt();
            switch (state)
            {
                case ChangesRecord.Remove:
                    {
                        var indexOf = IndexOf(key.NodeId);
                        if (indexOf >= 0)
                        {
                            Nodes.RemoveAt(indexOf);
                            ModuleFriend.OnRemoveNode(key);
                        }
                    }
                    break;

                case ChangesRecord.Put:
                    {
                        var value = new BLinkedMapNode();
                        value.Decode(bb);
                        var node = new BGetFriendNode();
                        node.NodeKey = key;
                        node.Node = value;
                        // 新节点加入：添加好友时，头部节点满了；或者活跃的好友数量超出了头部节点的容量
                        Nodes.Insert(0, node);
                        UpdateItemsSource(UpdateType.InsertHead, node);
                    }
                    break;

                case ChangesRecord.Edit:
                    {
                        var logBean = new HashSet<LogBean>();
                        bb.Decode(logBean);
                        var it = logBean.GetEnumerator();
                        if (it.MoveNext())
                        {
                            var indexOf = IndexOf(key.NodeId);
                            if (indexOf < 0)
                                return;
                            Nodes[indexOf].Node.FollowerApply(it.Current);
                            UpdateItemsSource(UpdateType.Update, Nodes[indexOf]);
                        }
                    }
                    break;
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
                    rpc.Argument.LinkedMapNameEndsWith = LinkedMapNameEndsWith;
                    return rpc;
                }
                else
                {
                    var rpc = new GetFriendNode();
                    rpc.Argument.NodeId = 0;
                    rpc.Argument.LinkedMapNameEndsWith = LinkedMapNameEndsWith;
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
                rpc.Argument.LinkedMapNameEndsWith = LinkedMapNameEndsWith;
                return rpc;
            }
            else
            {
                var rpc = new GetFriendNode();
                rpc.Argument.NodeId = 0;
                rpc.Argument.LinkedMapNameEndsWith = LinkedMapNameEndsWith;
                return rpc;
            }

        }

        public bool TryGetFriendNode(bool forward)
        {
            if (GetFriendNodePending != null)
                return true; // done

            GetFriendNodePending = TryNewGetFriendNode(forward);
            GetFriendNodePending?.Send(ModuleFriend.App.ClientService.GetSocket(), ProcessGetFriendNodeResponse);
            return GetFriendNodePending != null;
        }

        // 查询好友结果处理函数。
        private Task<long> ProcessGetFriendNodeResponse(Protocol p)
        {
            var r = p as GetFriendNode;
            if (r.ResultCode == 0)
            {
                GetFriendNodePending = null;
                var indexOf = IndexOf(r.Result.NodeKey.NodeId);
                if (indexOf >= 0)
                {
                    Nodes[indexOf] = r.Result;
                    UpdateItemsSource(UpdateType.Update, r.Result);
                }
                else
                {
                    Nodes.Add(r.Result);
                    UpdateItemsSource(UpdateType.InsertTail, r.Result);
                }
            }

            if (LoadAllCompleted != null)
            {
                // 失败马上触发结束回调；到达最后一个Node触发结束回调；
                if (r.ResultCode != 0 || false == TryGetFriendNode(true))
                    LoadAllCompleted();
            }

            return Task.FromResult(0L);
        }

        public void GetAllFriendNode(Action callback)
        {
            if (null != LoadAllCompleted)
                throw new InvalidOperationException();

            LoadAllCompleted = callback;
            TryGetFriendNode(true);
        }

        public bool FriendMatch(FriendItem ii, BLinkedMapNodeValue jj)
        {
            // todo 更多数据变化检查。需要结合User.Nick？
            var cur = (BFriend)jj.Value;
            var nick = string.IsNullOrEmpty(cur.Memo) ? jj.Id : cur.Memo;
            return ii.Nick.Equals(nick);
        }

        public FriendItem ToFriendItem(BLinkedMapNodeKey nodeKey, BLinkedMapNodeValue nodeValue)
        {
            var friend = (BFriend)nodeValue.Value;
            return new FriendItem()
            {
                NodeKey = nodeKey,
                Account = nodeValue.Id,
                Image = "https://www.google.com/images/hpp/Chrome_Owned_96x96.png",
                Nick = string.IsNullOrEmpty(friend.Memo) ? nodeValue.Id : friend.Memo,
                Time = "12:30",
                Message = "",
            };
        }

        public enum UpdateType
        {
            Update, // 更新
            InsertTail, // Nodes.Last的最后一项之后开始Insert
            InsertHead, // Nodes.First的第一项前面的index作为起始位置开始Insert
        }
        // 真值表          |  Topmost    | Friend 
        // Topmost Friend |  Head  Tail | Head  Tail 
        // empty   empty  |  0     0    | 0     0
        // empty   A,B    |  0     0    | @A    @B
        // E,F     empty  |  @E    @F   | Count Count
        // E,F     A,B    |  @E    @F   | @A    @B

        public int GetInsertTailIndex()
        {
            var itemsSource = ModuleFriend.ItemsSource;
            for (int n = Nodes.Count - 1; n >= 0; n--)
            {
                var lastNodeKey = Nodes[n].NodeKey;
                for (int i = itemsSource.Count - 1; i >= 0; i--)
                {
                    if (itemsSource[i].NodeKey.Equals(lastNodeKey))
                        return i;
                }
                // nodes is empty or all nodes.Values is empty or error(数据没有同步，这里忽略错误，继续处理)。
            }
            return FirstItemSource ? 0 : itemsSource.Count;
        }

        public int GetInsertHeadIndex()
        {
            var itemsSource = ModuleFriend.ItemsSource;
            for (int n = 0; n < Nodes.Count; n++)
            {
                var lastNodeKey = Nodes[n].NodeKey;
                for (int i = itemsSource.Count - 1; i >= 0; i--)
                {
                    if (itemsSource[i].NodeKey.Equals(lastNodeKey))
                        return i;
                }
                // nodes is empty or all nodes.Values is empty or error(数据没有同步，这里忽略错误，继续处理)。
            }
            return FirstItemSource ? 0 : itemsSource.Count;
        }

        private void InsertItemsSource(int insertIndex, BGetFriendNode node)
        {
            var itemsSource = ModuleFriend.ItemsSource;
            foreach (var friend in node.Node.Values)
            {
                var item = ToFriendItem(node.NodeKey, friend);
                logger.Debug($"Insert Friend {insertIndex} {item}");
                itemsSource.Insert(insertIndex++, item);
            }
        }

        private void UpdateItemsSource(BGetFriendNode node)
        {
            // 节点更新：节点内的项发生了变动。
            // 更新节点中的好友到View中。
            // 由于直接修改：ObservableCollection[i].Nick = "New Nick"；这种形式应该是没有通知View更新的。
            // 下面自己比较，尽可能优化少去引起View的更新。
            // 算法分析：
            // node中好友由于活动，可能会被提升到开头（可能不再这个节点，提到了更前面的节点）。
            // 下面从尾巴开始比较，可能在循环中删除。
            // 比较完全匹配，不更新ItemsSource；
            // 比较不匹配时，从ItemsSource中删除当前项；

            // reverse walk. maybe RemoveAt in loop.
            // 定位ItemsSource中是本节点好友的最后一个。
            var itemsSource = ModuleFriend.ItemsSource;
            int i = itemsSource.Count - 1;
            for (; i >= 0; --i)
            {
                var item = itemsSource[i];
                if (item.NodeKey.Equals(node.NodeKey))
                    break;
            }
            if (-1 == i)
                return; // impossible.

            // 比较friend数据是否改变
            int j = node.Node.Values.Count - 1;
            while (i >= 0 && j >= 0)
            {
                var ii = itemsSource[i];
                if (false == ii.NodeKey.Equals(node.NodeKey))
                    break; // view 中属于当前节点的item已经结束。

                var jj = node.Node.Values[j];

                if (ii.Account.Equals(jj.Id))
                {
                    if (false == FriendMatch(ii, jj))
                    {
                        // 数据发生了变更，使用删除再次加入的方式更新View。
                        //ItemsSource.RemoveAt(i);
                        //ItemsSource.Insert(i, FriendToItem(node.NodeId, jj));

                        // Replaced 可以正确通知View，不需要删除再加入。
                        itemsSource[i] = ToFriendItem(node.NodeKey, jj);
                    }
                    // 相同的好友，处理完成，都往前推进。
                    --i;
                    --j;
                }
                else
                {
                    // 不同的好友，删除View，往前推进，Node中的好友保持不变。
                    itemsSource.RemoveAt(i);
                    --i;
                }
            }

            // 删除ItemsSource中多出来的item
            for (; i >= 0; --i)
            {
                var ii = itemsSource[i];
                if (false == ii.NodeKey.Equals(node.NodeKey))
                    break; // view 中属于当前节点的item已经结束。
                itemsSource.RemoveAt(i);
            }

            // 添加Node中剩余的friend
            ++i; // 到这里时，i为-1，或者指向前面一个节点的最后一个好友。需要在这个后面开始插入剩余的friend。
            for (; j >= 0; --j)
            {
                itemsSource.Insert(i, ToFriendItem(node.NodeKey, node.Node.Values[j]));
            }
        }

        private void UpdateItemsSource(UpdateType updateType, BGetFriendNode node)
        {
            switch (updateType)
            {
                case UpdateType.InsertTail:
                    InsertItemsSource(GetInsertTailIndex(), node);
                    break;

                case UpdateType.InsertHead:
                    InsertItemsSource(GetInsertHeadIndex(), node);
                    break;

                case UpdateType.Update:
                    UpdateItemsSource(node);
                    break;
            }
        }
    }
}
