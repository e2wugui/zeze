using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Util;

namespace Zege.Message
{
    public class MessageFriend : Chat
    {
        public string Friend { get; }

        public MessageFriend(ModuleMessage module, string friend, IMessageView view)
            : base(module, view)
        {
            Friend = friend;

            var rpc = new GetFriendMessage();
            rpc.Argument.Friend = friend;
            rpc.Argument.MessageIdFrom = -2;
            rpc.Send(Module.App.ClientService.GetSocket(), Module.ProcessGetFriendMessageResponse);
        }

        public override bool IsYou(string account, long departmentId)
        {
            return account.Equals(Friend);
        }

        public override async Task DecryptMessage(BMessage message)
        {
            if (message.SecureKeyIndex >= 0)
            {
                var cert = await Module.App.Zege_User.LoadCertificate(message.SecureKeyIndex);
                message.SecureMessage = new Binary(Cert.DecryptRsa(cert, message.SecureMessage.GetBytesUnsafe()));
            }
        }

        public override async Task EncryptMessage(BMessage message)
        {
            var info = await Module.App.Zege_Friend.GetPublicUserInfo(Friend);
            if (info.Cert.Count > 0)
            {
                var cert = Cert.CreateFromPkcs12(info.Cert.GetBytesUnsafe(), "");
                var encryptedMessage = Cert.EncryptRsa(cert, message.SecureMessage.Bytes, message.SecureMessage.Offset, message.SecureMessage.Count);
                message.SecureMessage = new Binary(encryptedMessage);
                message.SecureKeyIndex = info.LastCertIndex;
            }
            else
            {
                // 未加密。
                message.SecureKeyIndex = -1;
            }
        }

        public override async Task SendAsync(string message)
        {
            var rpc = new SendMessage();
            rpc.Argument.Friend = Friend;

            FillTextMessage(rpc.Argument.Message, message);
            var notEncryptMessage = rpc.Argument.Message.SecureMessage;
            await EncryptMessage(rpc.Argument.Message);
            await rpc.SendAsync(Module.App.ClientService.GetSocket());

            if (0 == rpc.ResultCode)
            {
                // 给本地用之前恢复成不加密的。
                rpc.Argument.Message.SecureMessage = notEncryptMessage;

                // 自己发送的消息的这些变量是本地的，需要自己填写。
                // 服务器仅负责NotifyMessage的填写，收到别人的消息不需要填写。
                // 好友消息今天写这两个就够了。群消息还需要填写Group,DepartmentId。
                // 自己发送的消息服务器不Notify，是为了更大的灵活性。
                // 即发送者可以先把消息加入聊天窗口，等失败获成功时再更新状态。
                // 目前聊天窗口没有这个能力，所以等待服务器返回发送成功结果时才加入聊天窗口。
                rpc.Argument.Message.MessageId = rpc.Result.MessageId;
                rpc.Argument.Message.From = Module.App.Zege_User.Account;
                View.AddTail(rpc.Argument.Message);
            }
        }
    }
}
