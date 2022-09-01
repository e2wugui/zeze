using System.Collections.ObjectModel;

namespace Zege
{
    public partial class MainPage : ContentPage
    {
        public App App { get; private set; }

        public MainPage()
        {
            InitializeComponent();

            App = new App();
            App.Zege_Friend.Bind(FriendsListView);
        }

        private async void OnLoginClicked(object sender, EventArgs e)
        {
            App.Start("127.0.0.1", 5100);
            App.Connector.GetReadySocket(); // wait connection ready; TODO 改成异步。
            var account = Environment.MachineName.ToString();
            await App.Zege_User.OpenAsync(account);
            await App.Zege_Linkd.ChallengeFuture.Task;
            App.Zege_Friend.StartSyncData();

            var clientId = "PC";
            await App.Zeze_Builtin_Online.LoginAsync(clientId);

            // fix remove me
            // SemanticScreenReader.Announce(CounterBtn.Text);
        }
    }
}