namespace Zege
{
    public partial class AppShell : Shell
    {
        public AppShell()
        {
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

    }
}