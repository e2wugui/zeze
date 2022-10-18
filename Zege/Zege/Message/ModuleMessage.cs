
using Zeze.Util;

namespace Zege.Message
{
    public partial class ModuleMessage : AbstractModule
    {
        public void Start(global::Zege.App app)
        {
        }

        public void Stop(global::Zege.App app)
        {
        }

        protected override async System.Threading.Tasks.Task<long> ProcessNotifyMessageRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as NotifyMessage;
            return ResultCode.NotImplement;
        }

        public string Account { get; private set; } = string.Empty;
        public GraphicsView MessageView { get; private set; }
        public MessageDrawable Drawable { get; private set; }
        public void Bind(GraphicsView view)
        {
            if (null == MessageView)
            {
                MessageView = view;
                Drawable = new MessageDrawable();
                MessageView.Drawable = Drawable;
            }
        }

        public void ShowHistory(string account)
        {
            if (account.Equals(Account))
                return;

            Account = account;
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
