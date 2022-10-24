using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

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
            View = new MessageViewControl(Module.App.MainWindow.MessageParent);

            var rpc = new GetFriendMessage();
            rpc.Argument.Friend = friend;
            rpc.Argument.MessageIdFrom = -2;
            rpc.Send(Module.App.ClientService.GetSocket(), Module.ProcessGetFriendMessageResponse);
        }

        public void OnNotifyMessage(NotifyMessage p)
        {
            if (false == ReachEnd)
                return; // 本地消息历史没有包含最后一条消息，忽略新消息。等待用户翻页。

            // 处理最新的消息。
            if (Messages.TryAdd(p.Argument.MessageId, p.Argument))
                UpdateView(p.Argument, View.AddTail);

            NextMessageId = p.Argument.MessageId + 1;
            // TODO 需要检测当前View是否正在显示最新的消息，如果是，不需要更新红点。
            Module.App.MainWindow.UpdateRedPoint(Friend, NextMessageId - NextMessageIdNotRead);
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

            Module.App.MainWindow.UpdateRedPoint(Friend, NextMessageId - NextMessageIdNotRead);

            if (r.Result.Messages.Count == 0)
                return; // skip empty result; 可以不判断。

            if (IsNewMessage(r.Result.Messages))
            {
                foreach (var msg in r.Result.Messages)
                {
                    if (Messages.TryAdd(msg.MessageId, msg))
                        UpdateView(msg, View.AddTail);
                }
            }
            else
            {
                // 插入消息，倒过来遍历。
                for (var i = r.Result.Messages.Count - 1; i >= 0; --i)
                {
                    var msg = r.Result.Messages[i];
                    if (Messages.TryAdd(msg.MessageId, msg))
                        UpdateView(msg, View.InsertHead);
                }
            }
        }

        private void UpdateView(BMessage msg, Action<bool, long, string> action)
        {
            // 这两个分支以后再简化，先清晰点，写成这样了。
            switch (msg.Type)
            {
                case BMessage.eTypeText:
                    {
                        var text = new BTextMessage();
                        var self = msg.From.Equals(Module.App.Zege_User.Account);
                        action(self, msg.MessageId, text.Message);
                    }
                    break;

                case BMessage.eTypeSystem:
                    {
                    }
                    break;

                case BMessage.eTypeEmoji:
                    {
                    }
                    break;
            }
        }
    }
}
