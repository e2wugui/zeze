using System.Collections.ObjectModel;

namespace Zege
{
    public partial class MainPage : ContentPage
    {
        public App App { get; private set; }

        public MainPage()
        {
            InitializeComponent();

            AppDomain.CurrentDomain.UnhandledException += async (sender, error) =>
            {
                await DisplayAlert("UnhandledException", error.ExceptionObject.ToString(), "OK");
            };

            App = new App();
        }

        private async void OnLoginClicked(object sender, EventArgs e)
        {
            try
            {
                App.Start("127.0.0.1", 5100);
                App.Zege_Friend.Bind(FriendsListView);
                App.Connector.GetReadySocket(); // wait connection ready; TODO 改成异步。
                var account = Environment.MachineName.ToString();
                await App.Zege_User.OpenAsync(account);
                await App.Zege_Linkd.ChallengeFuture.Task;
                App.Zege_Friend.GetFristFriendNode();

                var clientId = "PC";
                await App.Zeze_Builtin_Online.LoginAsync(clientId);
            }
            catch (Exception ex)
            {
                await DisplayAlert("UnhandledException", ex.ToString(), "OK");
            }

            // fix remove me
            // SemanticScreenReader.Announce(CounterBtn.Text);
        }
    }
}