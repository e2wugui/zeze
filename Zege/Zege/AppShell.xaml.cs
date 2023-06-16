using NLog.Config;
using NLog.Targets;
using NLog;

namespace Zege
{
    public partial class AppShell : Shell
    {
        public static AppShell Instance => (AppShell)Microsoft.Maui.Controls.Application.Current.MainPage;

        public App App { get; private set; }

        public AppShell()
        {
            InitializeComponent();
            LoggingConfiguration();
            FollowerApplyTables.RegisterLog();

            App.OnError = async (title, message) =>
            {
                App = null;
                await DisplayAlertAsync(title, message);
            };
        }

        public App Login(string account, string passwd, bool savePassword)
        {
            App = App.Login(account, passwd, savePassword);
            return App;
        }

        public App Create(string account, string passwd, bool savePassword)
        {
            App = App.Create(account, passwd, savePassword);
            return App;
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