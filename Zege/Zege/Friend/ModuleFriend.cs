
using System.Collections.ObjectModel;
using Zeze.Net;
using Zeze.Transaction;
using Zeze.Util;

namespace Zege.Friend
{
    public partial class ModuleFriend : AbstractModule
    {
        public void Start(global::Zege.App app)
        {
        }

        public void Stop(global::Zege.App app)
        {
        }

        // 给ListView提供数据，可能是本地CachedNodes中好友的一部分。
        private ObservableCollection<FriendItem> ItemsSource { get; set; } = new();
        private ListView ListView { get; set; }

        // 置顶好友单独保存。
        // private BTopmostFriend Topmost;

        // 先实现仅从尾部添加和删除节点的方案。
        // 支持从头部删除下一步考虑。
        private List<BGetFriendNode> Nodes { get; } = new();
        private GetFriendNode GetFriendNodePending { get; set; }

        private GetFriendNode TryNewGetFriendNode(bool forward)
        {
            if (forward)
            {
                if (Nodes.Count > 0)
                {
                    var last = Nodes[^1];
                    if (last.NextNodeId == 0)
                        return null; // 已经是最后一个节点了。
                    var rpc = new GetFriendNode();
                    rpc.Argument.NodeId = last.NextNodeId;
                    return rpc;
                }
                // else 尝试获取第一个节点，如果用户没有任何好友节点，会一直尝试获取，TODO 处理一下？
                return new GetFriendNode();
            }

            if (Nodes.Count > 0)
            {
                var last = Nodes[0];
                if (last.PrevNodeId == 0)
                    return null; // 已经是最后一个节点了。
                var rpc = new GetFriendNode();
                rpc.Argument.NodeId = last.PrevNodeId;
                return rpc;
            }
            // else 尝试获取第一个节点，如果用户没有任何好友节点，会一直尝试获取，TODO 处理一下？
            return new GetFriendNode();

        }

        private void TryGetFriendNode(bool forward)
        {
            if (GetFriendNodePending != null)
                return; // done

            GetFriendNodePending = TryNewGetFriendNode(forward);
            GetFriendNodePending?.Send(App.ClientService.GetSocket(), ProcessGetFriendNodeResponse);
        }

        [DispatchMode(Mode = DispatchMode.UIThread)]
        private Task<long> ProcessGetFriendNodeResponse(Protocol p)
        {
            GetFriendNodePending = null;
            var r = p as GetFriendNode;
            if (r.ResultCode == 0)
            {
                UpdateItemsSource(r.Result);
            }
            return Task.FromResult(0L);
        }

        private void OnScrolled(object sender, ScrolledEventArgs args)
        {
            if (args.ScrollY > ListView.Height - 80)
                TryGetFriendNode(true);
            else if (args.ScrollY < 80)
                TryGetFriendNode(false);
        }

        public void Bind(ListView view)
        {
            ListView = view;
            view.ItemsSource = ItemsSource;
            view.Scrolled += OnScrolled;
        }

        public void GetFristFriendNodeAsync()
        {
            if (Nodes.Count == 0)
                TryGetFriendNode(true);
        }

        private int IndexOf(long nodeId)
        {
            for (int i = 0; i < Nodes.Count; ++i)
            {
                if (Nodes[i].NodeId == nodeId)
                    return i;
            }
            return -1;
        }

        private FriendItem FriendToItem(long nodeId, BGetFriend friend)
        {
            return new FriendItem()
            {
                NodeId = nodeId,
                Account = friend.Account,
                Image = "https://www.google.com/images/hpp/Chrome_Owned_96x96.png",
                Nick = friend.Memo + " " + friend.Nick,
                Time = "12:30",
                Message = "",
            };
        }

        private bool FriendMatch(FriendItem ii, BGetFriend jj)
        {
            // todo 更多数据变化检查。
            return ii.Nick.Equals(jj.Memo + " " + jj.Nick);
        }

        private void UpdateItemsSource(BGetFriendNode node)
        {
            var indexOf = IndexOf(node.NodeId);
            if (-1 == indexOf)
            {
                Nodes.Add(node);
                foreach (var friend in node.Friends)
                {
                    ItemsSource.Add(FriendToItem(node.NodeId, friend));
                }
            }
            else
            {
                Nodes[indexOf] = node; // replace

                // 更新节点中的好友到View中。
                // 由于直接修改：ObservableCollection[i].Nick = "New Nick"；这种形式应该是没有通知View更新的。
                // 所以下面自己比较，尽可能优化少去引起View的更新。
                // 算法分析：
                // node中好友由于活动，可能会被提升到开头（可能不再这个节点，提到了更前面的节点）。
                // 下面从尾巴开始比较，可能在循环中删除。
                // 比较完全匹配，不更新ItemsSource；
                // 比较不匹配时，从ItemsSource中删除当前项；

                // reverse walk. maybe RemoveAt in loop.
                // 定位ItemsSource中是本节点好友的最后一个。
                int i = ItemsSource.Count - 1;
                for (; i >= 0; --i)
                {
                    if (ItemsSource[i].NodeId == node.NodeId)
                        break;
                }
                if (-1 == i)
                    return; // impossible.

                // 比较friend数据是否改变
                int j = node.Friends.Count - 1;
                while (i >= 0 && j >= 0)
                {
                    var ii = ItemsSource[i];
                    if (ii.NodeId != node.NodeId)
                        break; // view 中属于当前节点的item已经结束。

                    var jj = node.Friends[j];

                    if (ii.Account.Equals(jj.Account))
                    {
                        if (FriendMatch(ii, jj))
                        {
                            // 数据发生了变更，使用删除再次加入的方式更新View。
                            ItemsSource.RemoveAt(i);
                            ItemsSource.Insert(i, FriendToItem(node.NodeId, jj));
                        }
                        // 相同的好友，处理完成，都往前推进。
                        --i;
                        --j;
                    }
                    else
                    {
                        // 不同的好友，删除View，往前推进，Node中的好友保持不变。
                        ItemsSource.RemoveAt(i);
                        --i;
                    }
                }

                // 删除ItemsSource中多出来的item
                for (; i >= 0; --i)
                {
                    var ii = ItemsSource[i];
                    if (ii.NodeId != node.NodeId)
                        break; // view 中属于当前节点的item已经结束。
                    ItemsSource.RemoveAt(i);
                }

                // 添加Node中剩余的friend
                ++i; // 到这里时，i为-1，或者指向前面一个节点的最后一个好友。需要在这个后面开始插入剩余的friend。
                for (; j >= 0; --j)
                {
                    ItemsSource.Insert(i, FriendToItem(node.NodeId, node.Friends[j]));
                }
            }
        }
    }

    public class FriendItem
    {
        // Basic
        public long NodeId { get; set; }
        public string Account { get; set; }

        // Bind To View
        public string Image { get; set; }
        public string Nick { get; set; }
        public string Time { get; set; }
        public string Message { get; set; }
    }
}
