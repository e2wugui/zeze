
using Java.Security;
using System.Collections.ObjectModel;
using Zeze.Net;
using static Android.App.LauncherActivity;

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
        private ObservableCollection<Friend> ItemsSource { get; set; } = new();
        private ListView ListView { get; set; }

        // 置顶好友单独保存。
        // private BTopmostFriend Topmost;

        // 先实现仅从尾部添加和删除节点的方案。
        // 支持从头部删除下一步考虑。
        private List<(long, BGetFriendNode)> Nodes { get; } = new();
        private GetFriendNode GetFriendNodePending { get; set; }

        private void TryGetNextFriendNode()
        {
            if (GetFriendNodePending != null)
                return; // done

            GetFriendNodePending = new GetFriendNode();
            GetFriendNodePending.Argument.NodeId = Nodes.Count > 0 ? Nodes[^1].Item2.NextNodeId : 0;
            GetFriendNodePending.Send(App.ClientService.GetSocket(), (p) =>
            {
                GetFriendNodePending = null;
                var r = p as GetFriendNode;
                if (r.ResultCode == 0)
                {
                    UpdateItemsSource(r.Argument.NodeId, r.Result);
                }
                return Task.FromResult(0L);
            });
        }

        private void OnScrolled(object sender, ScrolledEventArgs args)
        {
            if (args.ScrollY > ListView.Height - 80)
                TryGetNextFriendNode();
        }

        public void Bind(ListView view)
        {
            ListView = view;
            view.ItemsSource = ItemsSource;
            view.Scrolled += OnScrolled;

            if (Nodes.Count == 0)
                TryGetNextFriendNode();
        }

        private int IndexOf(long nodeId)
        {
            for (int i = 0; i < Nodes.Count; ++i)
            {
                if (Nodes[i].Item1 == nodeId)
                    return i;
            }
            return -1;
        }

        private Friend FriendToView(long nodeId, BGetFriend friend)
        {
            return new Friend()
            {
                NodeId = nodeId,
                Account = friend.Account,
                Image = "https://www.google.com/images/hpp/Chrome_Owned_96x96.png",
                Nick = friend.Memo + " " + friend.Nick,
                Time = "12:30",
                Message = "",
            };
        }

        private bool FriendMatch(Friend ii, BGetFriend jj)
        {
            // todo 更多数据变化检查。
            return ii.Nick.Equals(jj.Memo + " " + jj.Nick);
        }

        private void UpdateItemsSource(long nodeId, BGetFriendNode node)
        {
            var indexOf = IndexOf(nodeId);
            if (-1 == indexOf)
            {
                Nodes.Add((nodeId, node));
                foreach (var friend in node.Friends)
                {
                    ItemsSource.Add(FriendToView(nodeId, friend));
                }
            }
            else
            {
                Nodes[indexOf] = (nodeId, node); // replace

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
                    if (ItemsSource[i].NodeId == nodeId)
                        break;
                }
                if (-1 == i)
                    return; // impossible.

                // 比较friend数据是否改变
                int j = node.Friends.Count - 1;
                while (i >= 0 && j >= 0)
                {
                    var ii = ItemsSource[i];
                    if (ii.NodeId != nodeId)
                        break; // view 中属于当前节点的item已经结束。

                    var jj = node.Friends[j];

                    if (ii.Account.Equals(jj.Account))
                    {
                        if (FriendMatch(ii, jj))
                        {
                            // 数据发生了变更，使用删除再次加入的方式更新View。
                            ItemsSource.RemoveAt(i);
                            ItemsSource.Insert(i, FriendToView(nodeId, jj));
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
                    if (ii.NodeId != nodeId)
                        break; // view 中属于当前节点的item已经结束。
                    ItemsSource.RemoveAt(i);
                }

                // 添加Node中剩余的friend
                ++i; // 到这里时，i为-1，或者指向前面一个节点的最后一个好友。需要在这个后面开始插入剩余的friend。
                for (; j >= 0; --j)
                {
                    ItemsSource.Insert(i, FriendToView(nodeId, node.Friends[j]));
                }
            }
        }
    }

    public class Friend
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
