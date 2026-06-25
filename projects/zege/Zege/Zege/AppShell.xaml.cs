using NLog.Config;
using NLog.Targets;
using NLog;

namespace Zege
{
    public partial class AppShell : Shell
    {
        public static AppShell Instance => (AppShell)Microsoft.Maui.Controls.Application.Current.MainPage;

        public App App { get; private set; }

        public AppShell(App app)
        {
            this.App = app;
            App.OnError = async (title, message) =>
            {
                App = null;
                await DisplayAlertAsync(title, message);
            };

            InitializeComponent();

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