
using System.Collections.Concurrent;
using System.Collections.ObjectModel;
using System.Security.Cryptography.X509Certificates;
using Zege.Notify;
using Zeze.Builtin.Collections.LinkedMap;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Util;

namespace Zege.Friend
{
    public partial class ModuleFriend : AbstractModule
    {
        private readonly ConcurrentDictionary<string, BPublicUserInfo> PublicUserInfos = new(); // TODO 展开证书。

        public void Start(global::Zege.App app)
        {
            Topmosts = new FriendNodes(this, "@Zege.Topmost", true);
            Friends = new FriendNodes(this, "@Zege.Friend", false);
        }

        public async Task<BPublicUserInfo> GetPublicUserInfo(string account)
        {
            if (PublicUserInfos.TryGetValue(account, out var info))
                return info;

            // 并发执行的时候，可能会发起多个远程调用。【先不保护了。】
            var rpc = new GetPublicUserInfo();
            rpc.Argument.Account = account;
            await rpc.SendAndCheckResultCodeAsync(App.ClientService.GetSocket());
            PublicUserInfos[account] = rpc.Result; // put
            return rpc.Result;
        }

        public void Stop(global::Zege.App app)
        {
        }

        // 给UI提供数据，一般来说绑定到ListView。
        public ObservableCollection<FriendItem> ItemsSource { get; } = new();
        // 好友数据
        public FriendNodes Friends { get; private set; }
        public FriendNodes Topmosts { get; private set; }

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

        public void SetTopmost(FriendItem friend)
        {
            var r = new SetTopmost();
            r.Argument.Account = friend.Account;
            // 普通好友则设置Topmost，否则去除Topmost。
            r.Argument.Topmost = friend.NodeKey.Name.EndsWith(Friends.LinkedMapNameEndsWith);
            r.Send(App.ClientService.GetSocket());
        }

        public async void ReturnTop()
        {
            await AppShell.Instance.DisplayAlertAsync("!!!", "NotImplementedException");
        }

        public async Task<string> CreateGroup(IEnumerable<string> selected)
        { 
            var r = new CreateGroup();
            r.Argument.Members.Union(selected);

            var randomData = new byte[16];
            Random.Shared.NextBytes(randomData);
            r.Argument.RandomData = new Binary(randomData);

            var rsa = Cert.GenerateRsa();
            var sign = Cert.Sign(rsa, randomData);
            r.Argument.RsaPublicKey = new Binary(rsa.ExportRSAPublicKey());
            r.Argument.Signed = new Binary(sign);

            await r.SendAsync(App.Connector.TryGetReadySocket());
            Mission.VerifySkipResultCode(r.ResultCode);
            var group = r.Result.Group;

            var cert = Cert.CreateFromCertAndPrivateKey(r.Result.Cert.GetBytesUnsafe(), rsa);
            var pkcs12 = cert.Export(X509ContentType.Pkcs12, "");
            var base64 = Convert.ToBase64String(pkcs12);
            await SecureStorage.Default.SetAsync(group + "." + r.Result.LastCertIndex + ".pkcs12", base64);
            //await SecureStorage.Default.SetAsync(group + ".pkcs12", base64);

            var n = new SendNotify();
            n.Argument.Group = group;

            foreach (var account in selected)
            {
                var info = await App.Zege_Friend.GetPublicUserInfo(account); // TODO 优化，批量获取。
                if (info.Cert.Count > 0)
                {
                    var certTarget = Cert.CreateFromPkcs12(info.Cert.GetBytesUnsafe(), "");
                    var encryptedCert = Cert.EncryptRsa(certTarget, pkcs12, 0, pkcs12.Length);

                    var notify = new BNotify();
                    notify.Title = "Group Cert";
                    notify.Type = BNotify.eTypeGroupCert;
                    notify.Data = new Binary(encryptedCert);
                    notify.Properties["group"] = group;
                    notify.Properties["lastCertIndex"] = info.LastCertIndex.ToString();

                    n.Argument.Notifys[account] = notify;
                }
                // else TODO report warning message
            }
            await n.SendAsync(App.Connector.TryGetReadySocket());
            return group;
        }
    }

    public class FriendItem
    {
        // Context
        public BLinkedMapNodeKey NodeKey { get; set; }
        public string Account { get; set; }

        // Bind To View
        public string Image { get; set; }
        public string Nick { get; set; }
        public string Time { get; set; }
        public string Message { get; set; }
    }
}
