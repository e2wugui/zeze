
using System.Collections.Concurrent;
using System.Collections.ObjectModel;
using System.Security.Cryptography.X509Certificates;
using Zege.Message;
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
        public class PublicUserInfo
        { 
            public BPublicUserInfo Bean { get; set; }
            public X509Certificate2 Cert { get; set; }

            public PublicUserInfo(BPublicUserInfo info)
            {
                Bean = info;
                if (info.Cert.Count > 0)
                    Cert = Zeze.Util.Cert.CreateFromPkcs12(info.Cert.GetBytesUnsafe(), "");
            }

        }

        private readonly ConcurrentDictionary<string, PublicUserInfo> PublicUserInfos = new();

        public void Start(global::Zege.App app)
        {
            Topmosts = new FriendNodes(this, "@Zege.Topmost", true);
            Friends = new FriendNodes(this, "@Zege.Friend", false);
        }

        public async Task LoadPublicUserInfos(IEnumerable<string> accounts, bool refreshAll = false)
        {
            var r = new GetPublicUserInfos();

            if (refreshAll)
            {
                foreach (var account in accounts)
                    r.Argument.Accounts.Add(account);
            }
            else
            {
                foreach (var account in accounts)
                {
                    if (PublicUserInfos.ContainsKey(account))
                        continue;
                    r.Argument.Accounts.Add(account);
                }
            }

            if (r.Argument.Accounts.Count > 0)
            {
                await r.SendAsync(App.Connector.TryGetReadySocket());
                foreach (var e in r.Result.Infos)
                {
                    PublicUserInfos[e.Key] = new PublicUserInfo(e.Value);
                }
            }
        }

        public async Task<PublicUserInfo> GetPublicUserInfo(string account, bool refresh = false)
        {
            if (!refresh && PublicUserInfos.TryGetValue(account, out var info))
                return info;

            // 并发执行的时候，可能会发起多个远程调用。【先不保护了。】
            var rpc = new GetPublicUserInfo();
            rpc.Argument.Account = account;
            await rpc.SendAndCheckResultCodeAsync(App.ClientService.GetSocket());
            info = new PublicUserInfo(rpc.Result);
            PublicUserInfos[account] = info; // put
            return info;
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

        // 这个方法没有真正被使用。
        // 定义在这里是为了给GetFriendNode设置DispatchMode。
        // 真正的处理在FriendNodes里面实现。
        [DispatchMode(Mode = DispatchMode.UIThread)]
        internal Task<long> ProcessGetFriendNodeResponse(Protocol p)
        {
            return Task.FromResult(0L);
        }

        public async Task<long> SetTopmost(FriendItem friend)
        {
            var r = new SetTopmost();
            r.Argument.Account = friend.Account;
            // 普通好友则设置Topmost，否则去除Topmost。
            r.Argument.Topmost = friend.NodeKey.Name.EndsWith(Friends.LinkedMapNameEndsWith);
            await r.SendAsync(App.ClientService.GetSocket());
            return r.ResultCode;
        }

        public async Task ReturnTop()
        {
            await AppShell.Instance.DisplayAlertAsync("!!!", "NotImplementedException");
        }

        public async Task<long> SendGroupCertificate(string group, string account)
        {
            var notify = await EncryptGroupCertificateWith(group, account);
            if (notify == null)
                return ResultCode.Unknown;

            var n = new SendNotify();
            n.Argument.Group = group;
            n.Argument.Notifys[account] = notify;
            await n.SendAsync(App.Connector.TryGetReadySocket());
            return n.ResultCode;
        }

        private async Task<BNotify> EncryptGroupCertificateWith(string group, string account)
        {
            var pkcs12 = await App.Zege_User.GetLastPrivateCertificatePkcs12(group);
            if (pkcs12 == null)
                return null;

            var info = await App.Zege_Friend.GetPublicUserInfo(account);
            if (info.Cert != null)
            {
                var encryptedCert = Cert.EncryptAesWithRsa(info.Cert, pkcs12);

                var notify = new BNotify();
                notify.Title = "Group Cert";
                notify.Type = BNotify.eTypeGroupCert;
                notify.Data = new Binary(encryptedCert);
                notify.Properties["group"] = group;
                notify.Properties["lastCertIndex"] = info.Bean.LastCertIndex.ToString();
                return notify;
            }
            return null;
        }

        public async Task<string> CreateGroup(IEnumerable<string> selected)
        { 
            var r = new CreateGroup();
            r.Argument.Members.UnionWith(selected);

            var randomData = new byte[16];
            System.Random.Shared.NextBytes(randomData);
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
            await SecureStorage.Default.SetAsync(group + ".LastCertIndex", r.Result.LastCertIndex.ToString());

            await App.Zege_Friend.LoadPublicUserInfos(selected);

            var n = new SendNotify();
            n.Argument.Group = group;

            foreach (var account in selected)
            {
                var notify = await EncryptGroupCertificateWith(group, account);
                if (null != notify)
                    n.Argument.Notifys[account] = notify;
                // else warning
            }
            await n.SendAsync(App.Connector.TryGetReadySocket());
            return group;
        }

        public async Task<CreateDepartment> CreateDepartment(string group, long departmentId, string name)
        {
            var r = new CreateDepartment();
            r.Argument.Group = group;
            r.Argument.DepartmentId = departmentId;
            r.Argument.Name = name;

            await r.SendAsync(App.Connector.TryGetReadySocket());
            return r;
        }

        public async Task<long> AddDepartmentMember(string group, long departmentId, string account)
        {
            var r = new AddDepartmentMember();
            r.Argument.Group = group;
            r.Argument.DepartmentId = departmentId;
            r.Argument.Account = account;

            await r.SendAsync(App.Connector.TryGetReadySocket());
            return r.ResultCode;
        }

        public async Task<long> AddFriend(string account)
        {
            var r = new AddFriend();
            r.Argument.Account = account;
            await r.SendAsync(App.Connector.TryGetReadySocket());
            return r.ResultCode;
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

        public override string ToString()
        {
            return $"{Account}, {Nick}";
        }
    }
}
