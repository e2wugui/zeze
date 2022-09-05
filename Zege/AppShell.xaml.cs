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
    }
}