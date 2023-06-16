
using Zege.Friend;
using Zege.Message;
using Zege.User;

namespace Zege
{
    public partial class MainPage : ContentPage
    {
        public MainPage()
        {
            InitializeComponent();
        }

        public ScrollView MessageScrollView => _MessageScrollView;
        public AbsoluteLayout MessageLayout => _MessageLayout;
        public Layout MessageParent => _MessageParent;

        private void OnScrolled(object sender, ScrolledEventArgs args)
        {
            if (args.ScrollY > FriendsListView.Height - 120)
                AppShell.Instance.App.Zege_Friend.Friends.TryGetFriendNode(true);
        }

        private void OnFriendsItemSelected(object sender, EventArgs e)
        {
            var selected = FriendsListView.SelectedItem as FriendItem;
            if (null == selected)
                return;

            // TODO: make selected to top here

            AppShell.Instance.App.Zege_Message.StartChat(selected.Account);
        }

        public void UpdateRedPoint(string target, long notReadCount)
        {
            // TODO 更新未读红点。
        }

        private async void OnSendClicked(object sender, EventArgs e)
        {
            var message = MessageEditor.Text;
            await AppShell.Instance.App.Zege_Message.CurrentChat?.SendAsync(message);
            MessageEditor.Text = string.Empty;
        }

        private async void OnLoginClicked(object sender, EventArgs e)
        {
            var account = AccountEditor.Text;
            if (string.IsNullOrEmpty(account))
            {
                await DisplayAlert("Account", "Account Is Empty.", "Ok");
                return;
            }

            // TODO 应该有个登录窗口
            var app = AppShell.Instance.Login(account, PasswordEditor.Text, SavePasswordCheckBox.IsChecked);
            FriendsListView.ItemsSource = app.Zege_Friend.ItemsSource;
            FriendsListView.ItemSelected += OnFriendsItemSelected;
            FriendsListView.Scrolled += OnScrolled;
            app.Zege_Message.UpdateRedPoint = UpdateRedPoint;
            app.Zege_Message.MessageViewFactory = () => new MessageViewControl(app, MessageParent);
        }

        private async void OnCreateClicked(object sender, EventArgs e)
        {
            var account = AccountEditor.Text;
            if (string.IsNullOrEmpty(account))
            {
                await DisplayAlert("Account", "Account Is Empty.", "Ok");
                return;
            }

            // TODO 应该有个登录窗口
            var app = AppShell.Instance.Create(account, PasswordEditor.Text, SavePasswordCheckBox.IsChecked);
            FriendsListView.ItemsSource = app.Zege_Friend.ItemsSource;
            FriendsListView.ItemSelected += OnFriendsItemSelected;
            FriendsListView.Scrolled += OnScrolled;
            app.Zege_Message.UpdateRedPoint = UpdateRedPoint;
            app.Zege_Message.MessageViewFactory = () => new MessageViewControl(app, MessageParent);
        }

        private void OnClear(object sender, EventArgs e)
        {
            SecureStorage.Default.RemoveAll();
        }


        private void OnMakeCurrentFriendTop(object sender, EventArgs e)
        {
            var selected = FriendsListView.SelectedItem as FriendItem;
            if (null == selected)
                return;

            AppShell.Instance.App?.Zege_Friend.SetTopmost(selected);
        }

        private void OnReturnTop(object sender, EventArgs e)
        {
            AppShell.Instance.App?.Zege_Friend.ReturnTop();
        }

        private void OnTest(object sender, EventArgs e)
        {
            //App.Zege_Friend.Test();
            var message = MessageEditor.Text;
            if (string.IsNullOrEmpty(message))
                return;
            LabelMultiLine.Text = message;
        }

        private void OnAdd(object sender, EventArgs e)
        {
            var rpc = new AddFriend();
            rpc.Argument.Account = AccountEditor.Text;
            rpc.Argument.Memo = PasswordEditor.Text;
            rpc.Send(AppShell.Instance.App?.ClientService.GetSocket()); // skip rpc result
        }
    }
}