
using Zege.Friend;
using Zege.Message;
using Zege.User;

namespace Zege
{
    public partial class LoginPage : ContentPage
    {
        public LoginPage()
        {
            InitializeComponent();
        }

        private async void OnLoginClicked(object sender, EventArgs e)
        {
            var account = AccountEditor.Text;
            if (string.IsNullOrEmpty(account))
            {
                await DisplayAlert("Account", "Account Is Empty.", "Ok");
                return;
            }

            LoginShell.Login(account, PasswordEditor.Text, SavePasswordCheckBox.IsChecked);
        }

        private async void OnCreateClicked(object sender, EventArgs e)
        {
            var account = AccountEditor.Text;
            if (string.IsNullOrEmpty(account))
            {
                await DisplayAlert("Account", "Account Is Empty.", "Ok");
                return;
            }

            LoginShell.Create(account, PasswordEditor.Text, SavePasswordCheckBox.IsChecked);
        }

        private void OnClear(object sender, EventArgs e)
        {
            SecureStorage.Default.RemoveAll();
        }
    }
}