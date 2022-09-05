using NLog;
using NLog.Config;
using NLog.Targets;
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
            // fix remove me
            // SemanticScreenReader.Announce(CounterBtn.Text);

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
            Mission.Run(async () =>
            {
                await App.Connector.GetReadySocketAsync();
                var account = Environment.MachineName.ToString().ToLower();
                await App.Zege_User.TryCreateAsync(account, "123", true);
                await App.Zege_Linkd.ChallengeMeAsync();
                App.Zege_Friend.GetFristFriendNodeAsync();

                var clientId = "PC";
                await App.Zeze_Builtin_Online.LoginAsync(clientId);
            });
        }

        private void OnClear(object sender, EventArgs e)
        {
            SecureStorage.Default.RemoveAll();
        }
    }
}