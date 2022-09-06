
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
        public WebView WebView { get; private set; }

        public void Bind(WebView webview)
        {
            WebView = webview;
            WebView.Source = new HtmlWebViewSource() { Html = MessageHistoryHtml };
        }

        public void ShowHistory(string account)
        {
            if (account.Equals(Account))
                return;

            Account = account;
            WebView.Source = new HtmlWebViewSource() { Html = MessageHistoryHtml };
        }

        public void AddMessage(string message)
        {
            WebView.Eval($"addMessage({message})");
        }

        public const string MessageHistoryHtml =
                """
                <!DOCTYPE html>
                <html>
                <head>
                <meta charset="utf-8">
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
                    <table id="history">
                    </table>
                </body>
                </html>
                """;
    }
}
