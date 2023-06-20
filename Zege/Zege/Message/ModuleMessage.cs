
using System.Collections.Concurrent;
using Zeze.Transaction;
using Zeze.Util;

namespace Zege.Message
{
    public partial class ModuleMessage : AbstractModule
    {
        private ConcurrentDictionary<string, MessageFriend> Friends = new();
        private ConcurrentDictionary<BDepartmentKey, MessageGroup> Groups = new();

        public Chat CurrentChat { get; private set; }

        public Action<string, long> UpdateRedPoint { get; set; }
        public Func<IMessageView> MessageViewFactory { get; set; }

        public void Start(global::Zege.App app)
        {
        }

        public void Stop(global::Zege.App app)
        {
        }

        [DispatchMode(Mode = DispatchMode.UIThread)]
        protected override async Task<long> ProcessNotifyMessageRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as NotifyMessage;
            if (p.Argument.Group.Length == 0)
            {
                var friend = Friends.GetOrAdd(p.Argument.From, (key) => new MessageFriend(this, key, MessageViewFactory()));
                await friend.OnNotifyMessage(p);
            }
            else
            {
                var departmentKey = new BDepartmentKey(p.Argument.Group, p.Argument.DepartmentId);
                var group = Groups.GetOrAdd(departmentKey, (key) => new MessageGroup(this, key, MessageViewFactory()));
                await group.OnNotifyMessage(p);
            }
            return 0;
        }

        [DispatchMode(Mode = DispatchMode.UIThread)]
        internal async Task<long> ProcessGetFriendMessageResponse(Zeze.Net.Protocol p)
        {
            var r = p as GetFriendMessage;
            var friend = Friends.GetOrAdd(r.Argument.Friend, (key) => new MessageFriend(this, key, MessageViewFactory()));
            await friend.OnGetMessage(r.Result.NextMessageId, r.Result.ReachEnd, r.Result.NextMessageIdNotRead, r.Result.Messages);
            return 0L;
        }

        [DispatchMode(Mode = DispatchMode.UIThread)]
        internal async Task<long> ProcessGetGroupMessageResponse(Zeze.Net.Protocol p)
        {
            var r = p as GetGroupMessage;
            var department = Groups.GetOrAdd(r.Argument.GroupDepartment, (key) => new MessageGroup(this, key, MessageViewFactory()));
            await department.OnGetMessage(r.Result.NextMessageId, r.Result.ReachEnd, r.Result.NextMessageIdNotRead, r.Result.Messages);
            return 0L;
        }

        public void StartChat(string account, long departmentId)
        {
            if (null != CurrentChat && CurrentChat.IsYou(account, departmentId))
                return;

            if (account.EndsWith("@group"))
            {
                var departmentKey = new BDepartmentKey(account, departmentId);
                CurrentChat = Groups.GetOrAdd(departmentKey, (key) => new MessageGroup(this, key, MessageViewFactory()));
                CurrentChat.Show();
            }
            else
            {
                CurrentChat = Friends.GetOrAdd(account, (key) => new MessageFriend(this, key, MessageViewFactory()));
                CurrentChat.Show();
            }
        }

        /*
        public void AddMessage(string message)
        {
            var self = Random.Shared.Next(100) > 50;
            var className = self ? "class=\"SelfMessage\"" : string.Empty;
            message = message.Replace("\r", $"<br>");
            MessageView.Eval($"addMessage('<p {className}>{message}</p>')");
        }

        public const string MessageHistoryHtml =
                """
                <!DOCTYPE html>
                <html>
                <head>
                <meta charset="utf-8">
                <style type = "text/css"> 
                    .SelfMessage { 
                        text-align: right;
                        color: blue;
                    } 
                </style>
                <script>
                    function addMessage(message)
                    {
                        var history = document.getElementById("history");
                        var row = history.insertRow();
                        var col = row.insertCell();
                        col.innerHTML = message;
                    }
                </script>
                </head>
                <body>
                    <table id="history" style="width:100%">
                    </table>
                </body>
                </html>
                """;
        */
    }
}
