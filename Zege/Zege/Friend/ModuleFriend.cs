
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
            FriendTopmosts = new FriendNodes(this, "@Zege.Topmost", true);
            FriendNodes = new FriendNodes(this, "@Zege.Friend", false);
        }

        public void Stop(global::Zege.App app)
        {
        }

        // 给ListView提供数据，可能是本地CachedNodes中好友的一部分。
        internal ObservableCollection<FriendItem> ItemsSource { get; } = new();
        private ListView ListView { get; set; }

        // 好友数据
        private FriendNodes FriendNodes { get; set; }
        private FriendNodes FriendTopmosts { get; set; }

        // 置顶好友单独保存。
        private BTopmostFriends Topmosts;

        private FriendNodes GetFriendNodes(string tableName)
        {
            if (tableName.EndsWith(FriendNodes.LinkedMapNameEndsWith))
                return FriendNodes;

            if (tableName.EndsWith(FriendTopmosts.LinkedMapNameEndsWith))
                return FriendTopmosts;

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
                FriendNodes.TryGetFriendNode(true);
        }

        public void Bind(ListView view)
        {
            ListView = view;
            view.ItemsSource = ItemsSource;
            view.Scrolled += OnScrolled;
        }

        public void GetFristFriendNode()
        {
            if (FriendNodes.Nodes.Count == 0)
                FriendNodes.TryGetFriendNode(true);
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
            var r = new AddFriend();
            //r.Argument.Account = 
            //todo r.Send(App.ClientService.GetSocket(), ProcessAddNewFriend);
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
                FriendNodes.OnGetFriendNodeResponse(r.Result);
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
        public BLinkedMapNodeKey NodeKey { get; set; }
        public string Account { get; set; }

        // Bind To View
        public string Image { get; set; }
        public string Nick { get; set; }
        public string Time { get; set; }
        public string Message { get; set; }
    }
}
