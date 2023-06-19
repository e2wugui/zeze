using NLog.Config;
using NLog.Targets;
using NLog;
using Zege.User;

namespace Zege
{
    public partial class LoginShell : Shell
    {
        public static LoginShell Instance => (LoginShell)Microsoft.Maui.Controls.Application.Current.MainPage;

        public LoginShell()
        {
            InitializeComponent();
            LoggingConfiguration();
            FollowerApplyTables.RegisterLog();

            App.OnError = DisplayAlertAsync;
        }

        private static void OpenAppShell(App app)
        {
            Microsoft.Maui.Controls.Application.Current.MainPage = new AppShell(app);
        }

        public static App Login(string account, string passwd, bool savePassword)
        {
            var app = App.GetOrAdd(account);
            if (app.Zege_Linkd.ChallengeFuture.Task.IsCompletedSuccessfully)
                return app;

            Mission.Run(async () =>
            {
                if (await app.Zege_Linkd.ChallengeMeAsync(account, passwd, savePassword))
                {
                    app.Zege_Friend.GetFristFriendNode();
                    var clientId = "PC";
                    await app.Zeze_Builtin_Online.LoginAsync(clientId);
                    app.Zege_Notify.GetFirstNode();

                    OpenAppShell(app);
                }
                else
                {
                    app.Stop();
                    await App.OnError("Login", "Account Cert Not Exists."); ;
                }
            });
            return app;
        }

        public static App Create(string account, string passwd, bool save)
        {
            var app = App.GetOrAdd(account);
            Mission.Run(async () =>
            {
                await app.Connector.GetReadySocketAsync();
                var rc = await app.Zege_User.CreateAccountAsync(account, passwd, save);
                switch (rc)
                {
                    case 0:
                        Login(account, passwd, save);
                        break;

                    case ModuleUser.eAccountHasUsed:
                        app.Stop();
                        await App.OnError("Create Account", "Account Exists.");
                        break;

                    case ModuleUser.eAccountHasPrepared:
                        app.Stop();
                        await App.OnError("Create Account", "Account Busy.");
                        break;

                    default:
                        app.Stop();
                        await App.OnError("Create Account", $"Unknown Error{rc}");
                        break;
                }
            });
            return app;
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

        public async Task OnUnhandledException(Exception ex)
        {
            await DisplayAlert("Error", ex.Message + "\r\n" + ex.StackTrace, "Ok");
        }

        public async Task<string> GetPromptAsync(string title, string message)
        {
            if (MainThread.IsMainThread)
                return await DisplayPromptAsync(title, message);
            return await MainThread.InvokeOnMainThreadAsync(async () => await DisplayPromptAsync(title, message));
        }

        public async Task DisplayAlertAsync(string title, string message)
        {
            if (MainThread.IsMainThread)
                await DisplayAlert(title, message, "OK");
            else
                await MainThread.InvokeOnMainThreadAsync(async () => await DisplayAlert(title, message, "OK"));
        }
    }
}