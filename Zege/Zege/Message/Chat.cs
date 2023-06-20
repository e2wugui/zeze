using Zeze.Net;
using Zeze.Serialize;
using Zeze.Util;

namespace Zege.Message
{
    public abstract class Chat
    {
        public ModuleMessage Module { get; }
        public IMessageView View { get; }

        // 使用字典，可以去重（当查询返回重复的消息）。
        public SortedDictionary<long, BMessage> Messages = new();

        private long NextMessageIdNotRead;
        private long NextMessageId;
        private bool ReachEnd = false;

        public Chat(ModuleMessage module, IMessageView view)
        { 
            Module = module;
            View = view;
        }

        public abstract bool IsYou(string account, long departmentId);

        public void Show()
        {
            View.Show(NextMessageIdNotRead);
        }

        public async Task EncryptMessageWithAccountPublicKey(BMessage message, string account)
        {
            var info = await Module.App.Zege_Friend.GetPublicUserInfo(account);
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

        public async Task DecryptMessageWithAccountPrivateKey(BMessage message, string account)
        {
            if (message.SecureKeyIndex >= 0)
            {
                var cert = await Module.App.Zege_User.GetPrivateCertificate(account, message.SecureKeyIndex);
                if (cert != null)
                {
                    message.SecureMessage = new Binary(Cert.DecryptRsa(cert, message.SecureMessage.GetBytesUnsafe()));
                    return;
                }
                FillTextMessage(message, "这是一条加密消息，但解密失败。");
            }
            // 未加密消息，不用处理。
        }

        public abstract Task EncryptMessage(BMessage message);
        public abstract Task DecryptMessage(BMessage message);

        public void FillTextMessage(BMessage message, string text)
        {
            text = text.Replace("\r\n", "\n");
            text = text.Replace("\r", "\n");

            message.Type = BMessage.eTypeText;
            var textMessage = new BTextMessage() { Message = text };
            message.SecureMessage = new Binary(ByteBuffer.Encode(textMessage));
        }

        public abstract Task SendAsync(string message);

        public async Task OnNotifyMessage(NotifyMessage p)
        {
            if (false == ReachEnd)
                return; // 本地消息历史没有包含最后一条消息，忽略新消息。等待用户翻页。

            await DecryptMessage(p.Argument);
            // 处理最新的消息。
            if (Messages.TryAdd(p.Argument.MessageId, p.Argument))
                View.AddTail(p.Argument);

            NextMessageId = p.Argument.MessageId + 1;
            // TODO 需要检测当前View是否正在显示最新的消息，如果是，不需要更新红点。
            // Module.UpdateRedPoint(Friend, NextMessageId - NextMessageIdNotRead);
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

        public void OnGetMessage(long nextMessageId, bool reachEnd, long nextMessageIdNotRead, List<Zege.Message.BMessage> messages)
        {
            // 首先保存最新的全局信息。
            if (nextMessageIdNotRead > NextMessageIdNotRead)
                NextMessageIdNotRead = nextMessageIdNotRead;
            NextMessageId = nextMessageId;
            ReachEnd = reachEnd;

            //Module.UpdateRedPoint(Friend, NextMessageId - NextMessageIdNotRead);

            if (messages.Count == 0)
                return; // skip empty result; 可以不判断。

            if (IsNewMessage(messages))
            {
                foreach (var msg in messages)
                {
                    if (Messages.TryAdd(msg.MessageId, msg))
                        View.AddTail(msg);
                }
            }
            else
            {
                // 插入消息，倒过来遍历。
                for (var i = messages.Count - 1; i >= 0; --i)
                {
                    var msg = messages[i];
                    if (Messages.TryAdd(msg.MessageId, msg))
                        View.InsertHead(msg);
                }
            }
        }
    }
}
