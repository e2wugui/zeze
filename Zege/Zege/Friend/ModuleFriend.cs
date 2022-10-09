
using System.Collections.ObjectModel;
using Zeze.Builtin.Collections.LinkedMap;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Transaction.Collections;
using Zeze.Util;

namespace Zege.Friend
{
    public partial class ModuleFriend : AbstractModule
    {
        public void Start(global::Zege.App app)
        {
            FriendNodes = new FriendNodes(this, ""); // TODO table name
            FriendTopmosts = new FriendNodes(this, ""); // TODO table name
        }

        public void Stop(global::Zege.App app)
        {
        }

        // 给ListView提供数据，可能是本地CachedNodes中好友的一部分。
        private ObservableCollection<FriendItem> ItemsSource { get; set; } = new();
        private ListView ListView { get; set; }

        // 好友数据
        private FriendNodes FriendNodes { get; set; }
        private FriendNodes FriendTopmosts { get; set; }

        // 置顶好友单独保存。
        private BTopmostFriends Topmosts;

        [DispatchMode(Mode = DispatchMode.UIThread)]
        protected override Task<long> ProcessFriendNodeLogBeanNotify(Protocol p)
        {
            var r = p as FriendNodeLogBeanNotify;
            ChangesRecord.FollowerApply(ByteBuffer.Wrap(r.Argument.ChangeLog), (tableName) => FriendNodes); // TODO 根据表名选择表
            return Task.FromResult(ResultCode.Success);
        }

        private void OnScrolled(object sender, ScrolledEventArgs args)
        {
            if (args.ScrollY > ListView.Height - 120)
                FriendNodes.TryGetFriendNode(true);
            else if (args.ScrollY < 120)
                FriendNodes.TryGetFriendNode(false);
        }

        public void Bind(ListView view)
        {
            ListView = view;
            view.ItemsSource = ItemsSource;
            view.Scrolled += OnScrolled;
        }

        public void GetFristFriendNodeAsync()
        {
            if (FriendNodes.Nodes.Count == 0)
                FriendNodes.TryGetFriendNode(true);
        }

        private FriendItem FriendToItem(long nodeId, BLinkedMapNodeValue nodeValue)
        {
            var friend = (BFriend)nodeValue.Value;
            return new FriendItem()
            {
                NodeId = nodeId,
                Account = nodeValue.Id,
                Image = "https://www.google.com/images/hpp/Chrome_Owned_96x96.png",
                Nick = friend.Memo,
                Time = "12:30",
                Message = "",
            };
        }

        private bool FriendMatch(FriendItem ii, BLinkedMapNodeValue jj)
        {
            // todo 更多数据变化检查。需要结合User.Nick？
            return ii.Nick.Equals(((BFriend)jj.Value).Memo);
        }

        internal void OnRemoveNode(long nodeId)
        {
            int i = ItemsSource.Count - 1;

            // 从后面开始搜索这个节点的项
            for (; i >= 0; i--)
            {
                if (ItemsSource[i].NodeId == nodeId)
                    break;
            }

            // 删除节点中的项
            for (; i >= 0; i--)
            {
                var item = ItemsSource[i];
                if (item.NodeId != nodeId)
                    break;
                ItemsSource.RemoveAt(i);
            }
        }

        internal void UpdateItemsSource(int indexOf, FriendNodes friendNodes, BGetFriendNode node)
        {
            if (-1 == indexOf)
            {
                // 在尾部添加：浏览发现新的节点。
                friendNodes.Nodes.Add(node);
                foreach (var friend in node.Node.Values)
                {
                    ItemsSource.Add(FriendToItem(node.NodeId, friend));
                }
            }
            else if (-2 == indexOf)
            {
                // 在头部添加：新节点发生了。
                friendNodes.Nodes.Insert(0, node);
                var i = 0;
                foreach (var friend in node.Node.Values)
                {
                    ItemsSource.Insert(i++, FriendToItem(node.NodeId, friend));
                }
            }
            else
            {
                // 节点更新：节点内的项发生了变动。
                friendNodes.Nodes[indexOf] = node; // replace

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
                int i = ItemsSource.Count - 1;
                for (; i >= 0; --i)
                {
                    if (ItemsSource[i].NodeId == node.NodeId)
                        break;
                }
                if (-1 == i)
                    return; // impossible.

                // 比较friend数据是否改变
                int j = node.Node.Values.Count - 1;
                while (i >= 0 && j >= 0)
                {
                    var ii = ItemsSource[i];
                    if (ii.NodeId != node.NodeId)
                        break; // view 中属于当前节点的item已经结束。

                    var jj = node.Node.Values[j];

                    if (ii.Account.Equals(jj.Id))
                    {
                        if (FriendMatch(ii, jj)) // BUG：false == ；因为下面的RemoveAt第二次失败，先保留错误的代码。
                        {
                            // 数据发生了变更，使用删除再次加入的方式更新View。
                            //ItemsSource.RemoveAt(i);
                            //ItemsSource.Insert(i, FriendToItem(node.NodeId, jj));

                            // Replaced 可以正确通知View，不需要删除再加入。
                            ItemsSource[i] = FriendToItem(node.NodeId, jj);
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
                    ItemsSource.Insert(i, FriendToItem(node.NodeId, node.Node.Values[j]));
                }
            }
        }

        // Test Field
        static int NextFriendId = 0;

        private void GetTopmosts()
        {
            var r = new GetTopmostFriends();
            r.Send(App.ClientService.GetSocket(), ProcessGetTopmosts);
        }

        [DispatchMode(Mode = DispatchMode.UIThread)]
        private Task<long> ProcessGetTopmosts(Protocol p)
        {
            var r = p as GetTopmostFriends;
            if (r.ResultCode == 0)
            {
                Topmosts = r.Result;
            }
            // TODO Update ItemsSource
            return Task.FromResult(0L);
        }

        public void AddNewFriend()
        {
            var newFriend = new FriendItem();
            newFriend.Nick = "Friend " + NextFriendId++;
            newFriend.NodeId = FriendNodes.Nodes.Count;
            newFriend.Image = "https://www.google.com/images/hpp/Chrome_Owned_96x96.png";
            newFriend.Time = DateTime.Now.ToString();
            newFriend.Message = "";
            ItemsSource.Add(newFriend); // TODO: remove this

            var r = new AddFriend();
            //r.Argument.Account = 
            r.Send(App.ClientService.GetSocket(), ProcessAddNewFriend);
        }

        [DispatchMode(Mode = DispatchMode.UIThread)]
        private Task<long> ProcessAddNewFriend(Protocol p)
        {
            FriendNodes.GetFriendNodePending = null;
            var r = p as AddFriend;
            if (r.ResultCode == 0)
            {
                FriendNodes.TryGetFriendNode(false);
            }
            return Task.FromResult(0L);
        }

        [DispatchMode(Mode = DispatchMode.UIThread)]
        internal Task<long> ProcessGetFriendNodeResponse(Protocol p)
        {
            var r = p as GetFriendNode;
            if (r.ResultCode == 0)
            {
                FriendNodes.OnGetFriendNodeResponse(r.Result.NodeId, r.Result);
            }
            return Task.FromResult(0L);
        }

        public void DeleteTail()
        {
            // TODO
            //var r = new DeleteFriend();
            //r.Send(App.ClientService.GetSocket());
        }

        public void SetTopmostFriend(string account, bool topmost)
        {
            var r = new SetTopmostFriend();
            r.Argument.Account = account;
            r.Argument.Topmost = topmost;
            r.Send(App.ClientService.GetSocket());
        }

        public void ReturnTop()
        {
            throw new NotImplementedException();
        }

        public void Test()
        {
            var res = FriendNodes.TryNewGetFriendNode(true);
        }

        // Test Field

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
