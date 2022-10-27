
using System.Collections.ObjectModel;
using Zeze.Builtin.Collections.LinkedMap;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Util;

namespace Zege.Friend
{
    public partial class ModuleFriend : AbstractModule
    {
        public void Start(global::Zege.App app)
        {
            Topmosts = new FriendNodes(this, "@Zege.Topmost", true);
            Friends = new FriendNodes(this, "@Zege.Friend", false);
        }

        public void Stop(global::Zege.App app)
        {
        }

        // 给ListView提供数据。
        internal ObservableCollection<FriendItem> ItemsSource { get; } = new();
        private ListView ListView { get; set; }

        // 好友数据
        private FriendNodes Friends { get; set; }
        private FriendNodes Topmosts { get; set; }

        private FriendNodes GetFriendNodes(string tableName)
        {
            if (tableName.EndsWith(Friends.LinkedMapNameEndsWith))
                return Friends;

            if (tableName.EndsWith(Topmosts.LinkedMapNameEndsWith))
                return Topmosts;

            throw new NotImplementedException();
        }

        [DispatchMode(Mode = DispatchMode.UIThread)]
        protected override Task<long> ProcessFriendNodeLogBeanNotify(Protocol p)
        {
            // FollowerApply，可以全客户端只有一个协议。
            // 这里把好友相关的两个表的FollowerApply定义到一起。
            // 如果其他模块还需要FollowerApply，可以使用这个协议，也可以自己定义一个新的。
            // 是否使用同一个协议，看服务器通知的时候new哪一个协议。

            var r = p as FriendNodeLogBeanNotify;
            var bb = ByteBuffer.Wrap(r.Argument.ChangeLog);
            var tableName = bb.ReadString();
            var table = GetFriendNodes(tableName);
            var key = new BLinkedMapNodeKey();
            key.Decode(bb.ReadByteBuffer());
            table.FollowerApply(key, bb);
            return Task.FromResult(ResultCode.Success);
        }

        private void OnScrolled(object sender, ScrolledEventArgs args)
        {
            if (args.ScrollY > ListView.Height - 120)
                Friends.TryGetFriendNode(true);
        }

        public void Bind(ListView view)
        {
            ListView = view;
            view.ItemsSource = ItemsSource;
            view.Scrolled += OnScrolled;
        }

        public void GetFristFriendNode()
        {
            Topmosts.GetAllFriendNode(() =>
            {
                if (Friends.Nodes.Count == 0)
                    Friends.TryGetFriendNode(true);
            });
        }

        internal void OnRemoveNode(BLinkedMapNodeKey nodeKey)
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

        public void AddNewFriend()
        {
            var r = new AddFriend();
            //r.Argument.Account = 
            //todo r.Send(App.ClientService.GetSocket(), ProcessAddNewFriend);
        }

        [DispatchMode(Mode = DispatchMode.UIThread)]
        private Task<long> ProcessAddNewFriend(Protocol p)
        {
            Friends.GetFriendNodePending = null;
            var r = p as AddFriend;
            if (r.ResultCode == 0)
            {
                Friends.TryGetFriendNode(false);
            }
            return Task.FromResult(0L);
        }

        // 这个方法没有真正被使用。
        // 定义在这里是为了给GetFriendNode设置DispatchMode。
        // 真正的处理在FriendNodes里面实现。
        [DispatchMode(Mode = DispatchMode.UIThread)]
        internal Task<long> ProcessGetFriendNodeResponse(Protocol p)
        {
            return Task.FromResult(0L);
        }

        public void DeleteTail()
        {
            // TODO
            //var r = new DeleteFriend();
            //r.Send(App.ClientService.GetSocket());
        }

        public void SetTopmost(FriendItem friend)
        {
            var r = new SetTopmost();
            r.Argument.Account = friend.Account;
            // 普通好友则设置Topmost，否则去除Topmost。
            r.Argument.Topmost = friend.NodeKey.Name.EndsWith(Friends.LinkedMapNameEndsWith);
            r.Send(App.ClientService.GetSocket());
        }

        public void ReturnTop()
        {
            throw new NotImplementedException();
        }

        public void Test()
        {
            var res = Friends.TryNewGetFriendNode(true);
        }

        // Test Field

    }

    public class FriendItem
    {
        // Basic
        public BLinkedMapNodeKey NodeKey { get; set; }
        public string Account { get; set; }

        // Bind To View
        public string Image { get; set; }
        public string Nick { get; set; }
        public string Time { get; set; }
        public string Message { get; set; }
    }
}
