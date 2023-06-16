
using System.Collections.Concurrent;
using Zeze.Transaction;
using Zeze.Util;

namespace Zege.Message
{
    public partial class ModuleMessage : AbstractModule
    {
        private ConcurrentDictionary<string, MessageFriend> Friends = new();
        public MessageFriend CurrentChat { get; private set; }

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
            var friend = Friends.GetOrAdd(p.Argument.From, (key) => new MessageFriend(this, key, MessageViewFactory()));
            await friend.OnNotifyMessage(p);
            return 0;
        }

        [DispatchMode(Mode = DispatchMode.UIThread)]
        internal Task<long> ProcessGetFriendMessageResponse(Zeze.Net.Protocol p)
        {
            var r = p as GetFriendMessage;
            var friend = Friends.GetOrAdd(r.Argument.Friend, (key) => new MessageFriend(this, key, MessageViewFactory()));
            friend.OnGetFriendMessage(r);
            return Task.FromResult(0L);
        }

        public void StartChat(string account)
        {
            if (account.Equals(CurrentChat?.Friend))
                return;
            CurrentChat = Friends.GetOrAdd(account, (key) => new MessageFriend(this, key, MessageViewFactory()));
            CurrentChat.Show();
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
