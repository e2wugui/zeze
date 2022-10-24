
using System.Collections.Concurrent;
using Zeze.Transaction;
using Zeze.Util;

namespace Zege.Message
{
    public partial class ModuleMessage : AbstractModule
    {
        private ConcurrentDictionary<string, MessageFriend> Friends = new();

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
            var friend = Friends.GetOrAdd(p.Argument.From, (key) => new MessageFriend(this, key));
            friend.OnNotifyMessage(p);
            return ResultCode.NotImplement;
        }

        public string Account { get; private set; } = string.Empty;
        public GraphicsView MessageView { get; private set; }

        public void Bind(GraphicsView view)
        {
            if (null == MessageView)
            {
                MessageView = view;
            }
        }

        public void SwitchChat(string account)
        {
            if (account.Equals(Account))
                return;

            Account = account;

            var rpc = new GetFriendMessage();
            rpc.Argument.Friend = account;
            rpc.Send(App.ClientService.GetSocket(), ProcessGetFriendMessageResponse);
        }

        [DispatchMode(Mode = DispatchMode.UIThread)]
        internal async Task<long> ProcessGetFriendMessageResponse(Zeze.Net.Protocol p)
        {
            var r = p as GetFriendMessage;
            var friend = Friends.GetOrAdd(r.Argument.Friend, (key) => new MessageFriend(this, key));
            friend.OnGetFriendMessage(r);
            return 0;
        }

        public void AddMessage(string message)
        {
            /*
            var self = Random.Shared.Next(100) > 50;
            var className = self ? "class=\"SelfMessage\"" : string.Empty;
            message = message.Replace("\r", $"<br>");
            MessageView.Eval($"addMessage('<p {className}>{message}</p>')");
            */
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
    }
}
