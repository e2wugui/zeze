
using Zege.Friend;
using Zeze.Builtin.Collections.LinkedMap;
using Zeze.Serialize;
using Zeze.Transaction.Collections;
using Zeze.Transaction;
using Zeze.Util;
using System.Collections.ObjectModel;
using Zeze.Net;

namespace Zege.Notify
{
    public partial class ModuleNotify : AbstractModule
    {
        public List<BGetNotifyNode> Nodes { get; } = new();
        internal ObservableCollection<NotifyItem> ItemsSource { get; } = new();
        public NotifyPage NotifyPage { get; private set; }
        public GetNotifyNode GetNotifyNodePending { get; set; }


        public void Start(global::Zege.App app)
        {
        }

        public void Stop(global::Zege.App app)
        {
        }

        public void GetFirstNode()
        {
            if (Nodes.Count == 0)
                TryGetFriendNode(true);
        }

        internal GetNotifyNode TryNewGetFriendNode(bool forward)
        {
            if (forward)
            {
                if (Nodes.Count > 0)
                {
                    var last = Nodes[^1];
                    if (last.Node.NextNodeId == 0)
                        return null; // 已经是最后一个节点了。
                    var rpc = new GetNotifyNode();
                    rpc.Argument.NodeId = last.Node.NextNodeId;
                    return rpc;
                }
                else
                {
                    var rpc = new GetNotifyNode();
                    rpc.Argument.NodeId = 0;
                    return rpc;
                }
            }

            if (Nodes.Count > 0)
            {
                var last = Nodes[0];
                if (last.Node.PrevNodeId == 0)
                    return null; // 已经是最后一个节点了。
                var rpc = new GetNotifyNode();
                rpc.Argument.NodeId = last.Node.PrevNodeId;
                return rpc;
            }
            else
            {
                var rpc = new GetNotifyNode();
                rpc.Argument.NodeId = 0;
                return rpc;
            }

        }

        public bool TryGetFriendNode(bool forward)
        {
            if (GetNotifyNodePending != null)
                return true; // done

            GetNotifyNodePending = TryNewGetFriendNode(forward);
            GetNotifyNodePending?.Send(App.ClientService.GetSocket(), ProcessGetNotifyNodeResponse);
            return GetNotifyNodePending != null;
        }

        // 查询好友结果处理函数。
        [DispatchMode(Mode = DispatchMode.UIThread)]
        private Task<long> ProcessGetNotifyNodeResponse(Protocol p)
        {
            var r = p as GetNotifyNode;
            if (r.ResultCode == 0)
            {
                GetNotifyNodePending = null;
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
            return Task.FromResult(0L);
        }

        public void SetNotifyPage(NotifyPage page)
        {
            if (null != NotifyPage)
                NotifyPage.NotifyListView.ItemsSource = null; // detach
            NotifyPage = page;
            if (null != NotifyPage)
                NotifyPage.NotifyListView.ItemsSource = ItemsSource; // attach
        }

        protected override Task<long> ProcessNotifyNodeLogBeanNotify(Zeze.Net.Protocol _p)
        {
            var p = _p as NotifyNodeLogBeanNotify;

            var bb = ByteBuffer.Wrap(p.Argument.ChangeLog);
            var _ = bb.ReadString(); // Read TableName. Skip.
            var key = new BLinkedMapNodeKey();
            key.Decode(bb.ReadByteBuffer());
            FollowerApply(key, bb);
            return Task.FromResult(ResultCode.Success);
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
                            OnRemoveNode(key);
                        }
                    }
                    break;

                case ChangesRecord.Put:
                    {
                        var value = new BLinkedMapNode();
                        value.Decode(bb);
                        var node = new BGetNotifyNode();
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

        public int IndexOf(long nodeId)
        {
            for (int i = 0; i < Nodes.Count; ++i)
            {
                if (Nodes[i].NodeKey.NodeId == nodeId)
                    return i;
            }
            return -1;
        }

        private void OnRemoveNode(BLinkedMapNodeKey nodeKey)
        {
            int i = ItemsSource.Count - 1;

            // 从后面开始搜索这个节点的项
            for (; i >= 0; i--)
            {
                var item = ItemsSource[i];
                if (item.NodeKey.Equals(nodeKey))
                    break;
            }

            // 删除节点中的项
            for (; i >= 0; i--)
            {
                var item = ItemsSource[i];
                if (false == item.NodeKey.Equals(nodeKey))
                    break;
                ItemsSource.RemoveAt(i);
            }
        }

        public bool NotifyMatch(NotifyItem ii, BLinkedMapNodeValue jj)
        {
            // todo 更多数据变化检查。需要结合User.Nick？
            var cur = (BNotify)jj.Value;
            var nick = string.IsNullOrEmpty(cur.Title) ? jj.Id : cur.Title;
            return ii.Title.Equals(nick);
        }

        public NotifyItem ToNotifyItem(BLinkedMapNodeKey nodeKey, BLinkedMapNodeValue nodeValue)
        {
            var notify = (BNotify)nodeValue.Value;
            return new NotifyItem()
            {
                NodeKey = nodeKey,
                Id = nodeValue.Id,
                Title = notify.Title,
            };
        }

        public enum UpdateType
        {
            Update, // 更新
            InsertTail, // Nodes.Last的最后一项之后开始Insert
            InsertHead, // Nodes.First的第一项前面的index作为起始位置开始Insert
        }

        private void InsertItemsSource(int insertIndex, BGetNotifyNode node)
        {
            var itemsSource = ItemsSource;
            foreach (var friend in node.Node.Values)
            {
                itemsSource.Insert(insertIndex++, ToNotifyItem(node.NodeKey, friend));
            }
        }

        private void UpdateItemsSource(BGetNotifyNode node)
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
            var itemsSource = ItemsSource;
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

                if (ii.Id.Equals(jj.Id))
                {
                    if (false == NotifyMatch(ii, jj))
                    {
                        // 数据发生了变更，使用删除再次加入的方式更新View。
                        //ItemsSource.RemoveAt(i);
                        //ItemsSource.Insert(i, FriendToItem(node.NodeId, jj));

                        // Replaced 可以正确通知View，不需要删除再加入。
                        itemsSource[i] = ToNotifyItem(node.NodeKey, jj);
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
                itemsSource.Insert(i, ToNotifyItem(node.NodeKey, node.Node.Values[j]));
            }
        }

        private void UpdateItemsSource(UpdateType updateType, BGetNotifyNode node)
        {
            switch (updateType)
            {
                case UpdateType.InsertTail:
                    InsertItemsSource(ItemsSource.Count, node);
                    break;

                case UpdateType.InsertHead:
                    InsertItemsSource(0, node);
                    break;

                case UpdateType.Update:
                    UpdateItemsSource(node);
                    break;
            }
        }
    }

    public class NotifyItem
    {
        // Basic
        public BLinkedMapNodeKey NodeKey { get; set; }
        public string Id { get; set; }

        // Bind To View
        public string Title { get; set; }
        public string ExpireTime { get; set; }
    }
}
