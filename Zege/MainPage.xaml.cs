
using NLog;
using NLog.Config;
using NLog.Targets;
using Zege.User;

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

        private void StartApp()
        {
            if (App == null)
            {
                App = new App();
                App.Start("127.0.0.1", 5100);
                App.Zege_Friend.Bind(FriendsListView);
            }
        }

        private void OnLoginClicked(object sender, EventArgs e)
        {
            StartApp();

            var account = EditorAccount.Text;
            var passwrd = EditorPassword.Text;
            var save = CheckBoxSavePassword.IsChecked;

            Mission.Run(async () =>
            {
                if (await App.Zege_Linkd.ChallengeMeAsync(account, passwrd, save))
                {
                    App.Zege_Friend.GetFristFriendNodeAsync();
                    var clientId = "PC";
                    await App.Zeze_Builtin_Online.LoginAsync(clientId);
                }
                else
                {
                    await Mission.AppShell.DisplayAlertAsync("Login", "Account Cert Not Exists."); ;
                }
            });
        }

        private void OnCreateClicked(object sender, EventArgs e)
        {
            StartApp();

            var account = EditorAccount.Text;
            var passwrd = EditorPassword.Text;
            var save = CheckBoxSavePassword.IsChecked;

            Mission.Run(async () =>
            {
                await App.Connector.GetReadySocketAsync();
                var rc = await App.Zege_User.CreateAccountAsync(account, passwrd, save);
                switch (rc)
                {
                    case 0:
                        OnLoginClicked(sender, e);
                        break;

                    case ModuleUser.eAccountHasUsed:
                        await Mission.AppShell.DisplayAlertAsync("Create Account", "Account Exists.");
                        break;

                    case ModuleUser.eAccountHasPrepared:
                        await Mission.AppShell.DisplayAlertAsync("Create Account", "Account Busy.");
                        break;

                    default:
                        await Mission.AppShell.DisplayAlertAsync("Create Account", $"Unknown Error{rc}");
                        break;
                }

            });
        }

        private void OnClear(object sender, EventArgs e)
        {
            SecureStorage.Default.RemoveAll();
        }
    }
}