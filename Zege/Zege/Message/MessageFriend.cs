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
    public class MessageFriend
    {
        public ModuleMessage Module { get; }
        public string Friend { get; }
        public IMessageView View { get; }

        // 使用字典，可以去重（当查询返回重复的消息）。
        public SortedDictionary<long, BMessage> Messages = new();

        private long NextMessageIdNotRead;
        private long NextMessageId;
        private bool ReachEnd = false;

        // target is friend or group
        // 未来可能分成两个实现。
        public MessageFriend(ModuleMessage module, string friend)
        {
            Module = module;
            Friend = friend;
            View = new MessageViewControl(Module.App, Module.App.MainPage.MessageParent);

            var rpc = new GetFriendMessage();
            rpc.Argument.Friend = friend;
            rpc.Argument.MessageIdFrom = -2;
            rpc.Send(Module.App.ClientService.GetSocket(), Module.ProcessGetFriendMessageResponse);
        }

        public async Task OnNotifyMessage(NotifyMessage p)
        {
            if (false == ReachEnd)
                return; // 本地消息历史没有包含最后一条消息，忽略新消息。等待用户翻页。

            if (p.Argument.SecureKeyIndex >= 0)
            {
                var cert = await Module.App.Zege_User.LoadCertificate(p.Argument.SecureKeyIndex);
                p.Argument.SecureMessage = new Binary(Cert.DecryptRsa(cert, p.Argument.SecureMessage.GetBytesUnsafe()));
            }
            // 处理最新的消息。
            if (Messages.TryAdd(p.Argument.MessageId, p.Argument))
                View.AddTail(p.Argument);

            NextMessageId = p.Argument.MessageId + 1;
            // TODO 需要检测当前View是否正在显示最新的消息，如果是，不需要更新红点。
            Module.App.MainPage.UpdateRedPoint(Friend, NextMessageId - NextMessageIdNotRead);
        }

        private bool IsNewMessage(List<BMessage> messages)
        {
            if (Messages.Count == 0)
                return true;
            if (messages.Count == 0)
                return true;
            if (messages.Last().MessageId > Messages.Last().Key)
                return true;
            return false;
        }

        public void OnGetFriendMessage(GetFriendMessage r)
        {
            // 首先保存最新的全局信息。
            if (r.Result.NextMessageIdNotRead > NextMessageIdNotRead)
                NextMessageIdNotRead = r.Result.NextMessageIdNotRead;
            NextMessageId = r.Result.NextMessageId;
            ReachEnd = r.Result.ReachEnd;

            Module.App.MainPage.UpdateRedPoint(Friend, NextMessageId - NextMessageIdNotRead);

            if (r.Result.Messages.Count == 0)
                return; // skip empty result; 可以不判断。

            if (IsNewMessage(r.Result.Messages))
            {
                foreach (var msg in r.Result.Messages)
                {
                    if (Messages.TryAdd(msg.MessageId, msg))
                        View.AddTail(msg);
                }
            }
            else
            {
                // 插入消息，倒过来遍历。
                for (var i = r.Result.Messages.Count - 1; i >= 0; --i)
                {
                    var msg = r.Result.Messages[i];
                    if (Messages.TryAdd(msg.MessageId, msg))
                        View.InsertHead(msg);
                }
            }
        }

        public void Show()
        {
            View.Show(NextMessageIdNotRead);
        }

        public async Task SendAsync(string message)
        {
            message = message.Replace("\r\n", "\n");
            message = message.Replace("\r", "\n");

            var rpc = new SendMessage();
            rpc.Argument.Friend = Friend;
            rpc.Argument.Message.Type = BMessage.eTypeText;
            var textMessage = new BTextMessage() { Message = message };
            // 
            rpc.Argument.Message.SecureMessage = new Binary(ByteBuffer.Encode(textMessage));

            await rpc.SendAsync(Module.App.ClientService.GetSocket());
            if (0 == rpc.ResultCode)
            {
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
