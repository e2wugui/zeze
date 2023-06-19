
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

            var app = AppShell.Instance.App;
            FriendsListView.ItemsSource = app.Zege_Friend.ItemsSource;
            FriendsListView.ItemSelected += OnFriendsItemSelected;
            FriendsListView.Scrolled += OnScrolled;
            app.Zege_Message.UpdateRedPoint = UpdateRedPoint;
            app.Zege_Message.MessageViewFactory = () => new MessageViewControl(app, MessageParent);
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

            AppShell.Instance.App.Zege_Message.StartChat(selected.Account, 0);
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

        private async void OnAdd(object sender, EventArgs e)
        {
            var rpc = new AddFriend();
            rpc.Argument.Account = await AppShell.Instance.GetPromptAsync("account", "");
            rpc.Argument.Memo = await AppShell.Instance.GetPromptAsync("memo", "");
            rpc.Send(AppShell.Instance.App?.ClientService.GetSocket()); // skip rpc result
        }
    }
}