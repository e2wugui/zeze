using NLog;
using NLog.Config;
using NLog.Targets;
using System.Collections.ObjectModel;
using Zeze.Util;

namespace Zege
{
    public partial class MainPage : ContentPage
    {
        public App App { get; private set; }

        public MainPage()
        {
            InitializeComponent();
            //SecureStorage.Default.SetAsync("", "");

            AppDomain.CurrentDomain.UnhandledException += async (sender, args) =>
            {
                await DisplayAlert("UnhandledException", args.ExceptionObject.ToString(), "OK");
            };

            LoggingConfiguration();
            App = new App();
            App.Start("127.0.0.1", 5100);
            App.Zege_Friend.Bind(FriendsListView);
        }

        private void LoggingConfiguration()
        {
            var config = new LoggingConfiguration();
            var fileTarget = new FileTarget
            {
                FileName = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory), "zege.log"),
                Layout = "${longdate} ${level} ${message} ${exception:format=Message,StackTrace}"
            };
            config.AddTarget("file", fileTarget);
            config.LoggingRules.Add(new LoggingRule("*", LogLevel.Trace, fileTarget));
            LogManager.Configuration = config;
        }

        private void OnLoginClicked(object sender, EventArgs e)
        {
            _ = Mission.CallAsync(async () =>
            {
                App.Connector.GetReadySocket(); // wait connection ready; TODO 改成异步。
                var account = Environment.MachineName.ToString();
                await App.Zege_User.OpenAsync(account);
                await App.Zege_Linkd.ChallengeFuture.Task;
                App.Zege_Friend.GetFristFriendNode();

                var clientId = "PC";
                await App.Zeze_Builtin_Online.LoginAsync(clientId);
            }, "OnLoginClicked");

            // fix remove me
            // SemanticScreenReader.Announce(CounterBtn.Text);
        }
    }
}