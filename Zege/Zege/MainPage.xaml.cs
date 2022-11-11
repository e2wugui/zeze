
using NLog;
using NLog.Config;
using NLog.Targets;
using System.Collections.ObjectModel;
using Zege.Friend;
using Zege.Message;
using Zege.User;
using Zeze.Transaction;
using Zeze.Transaction.Collections;

namespace Zege
{
    public partial class MainPage : ContentPage
    {
        public App App { get; private set; }

        public MainPage()
        {
            InitializeComponent();
            LoggingConfiguration();
            FollowerApplyTables.RegisterLog();
        }

        public ScrollView MessageScrollView => _MessageScrollView;
        public AbsoluteLayout MessageLayout => _MessageLayout;
        public Layout MessageParent => _MessageParent;
        public Window NotifyWindow { get; private set; }

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

        private void StartApp()
        {
            if (App == null)
            {
                App = new App();
                App.MainPage = this;
                App.Start("127.0.0.1", 5100);
                App.Zege_Friend.Bind(FriendsListView);
                FriendsListView.ItemSelected += OnFriendsItemSelected;
            }
        }

        private void OnFriendsItemSelected(object sender, EventArgs e)
        {
            var selected = FriendsListView.SelectedItem as FriendItem;
            if (null == selected)
                return;

            // TODO: make selected to top here

            App.Zege_Message.StartChat(selected.Account);
        }

        public void UpdateRedPoint(string target, long notReadCount)
        {
            // TODO 更新未读红点。
        }

        private async void OnSendClicked(object sender, EventArgs e)
        {
            var message = MessageEditor.Text;
            await App.Zege_Message.CurrentChat?.SendAsync(message);
            MessageEditor.Text = string.Empty;
        }

        private async void OnLoginClicked(object sender, EventArgs e)
        {
            StartApp();

            var account = AccountEditor.Text;
            if (string.IsNullOrEmpty(account))
            {
                await DisplayAlert("Account", "Account Is Empty.", "Ok");
                return;
            }
            var passwd = PasswordEditor.Text;
            var save = SavePasswordCheckBox.IsChecked;

            if (App.Zege_Linkd.ChallengeFuture.Task.IsCompletedSuccessfully)
                return;

            Mission.Run(async () =>
            {
                if (await App.Zege_Linkd.ChallengeMeAsync(account, passwd, save))
                {
                    App.Zege_Friend.GetFristFriendNode();
                    var clientId = "PC";
                    await App.Zeze_Builtin_Online.LoginAsync(clientId);
                    App.Zege_Notify.GetFirstNode();
                }
                else
                {
                    await Mission.AppShell.DisplayAlertAsync("Login", "Account Cert Not Exists."); ;
                }
            });
        }

        private async void OnCreateClicked(object sender, EventArgs e)
        {
            StartApp();

            var account = AccountEditor.Text;
            if (string.IsNullOrEmpty(account))
            {
                await DisplayAlert("Account", "Account Is Empty.", "Ok");
                return;
            }
            var passwd = PasswordEditor.Text;
            var save = SavePasswordCheckBox.IsChecked;

            Mission.Run(async () =>
            {
                await App.Connector.GetReadySocketAsync();
                var rc = await App.Zege_User.CreateAccountAsync(account, passwd, save);
                switch (rc)
                {
                    case 0:
                        OnLoginClicked(sender, e);
                        break;

                    case ModuleUser.eAccountHasUsed:
                        await Mission.AppShell.DisplayAlertAsync("Create Account", "Account Exists.");
                        break;

                    case ModuleUser.eAccountHasPrepared:
                        await Mission.AppShell.DisplayAlertAsync("Create Account", "Account Busy.");
                        break;

                    default:
                        await Mission.AppShell.DisplayAlertAsync("Create Account", $"Unknown Error{rc}");
                        break;
                }
            });
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

            App.Zege_Friend.SetTopmost(selected);
        }

        private void OnReturnTop(object sender, EventArgs e)
        {
            App.Zege_Friend.ReturnTop();
        }

        private void OnTest(object sender, EventArgs e)
        {
            //App.Zege_Friend.Test();
            var message = MessageEditor.Text;
            if (string.IsNullOrEmpty(message))
                return;
            LabelMultiLine.Text = message;
        }

        private void OnOpenNotifyWindow(object sender, EventArgs e)
        {
            if (null == NotifyWindow && null != App)
            {
                var notifyPage = new Zege.Notify.NotifyPage(App);
                App.Zege_Notify.SetNotifyPage(notifyPage);
                NotifyWindow = new Window(notifyPage);
                NotifyWindow.Destroying += NotifyWindow_Destroying;
                Microsoft.Maui.Controls.Application.Current.OpenWindow(NotifyWindow);
            }
        }

        private void NotifyWindow_Destroying(object sender, EventArgs e)
        {
            App.Zege_Notify.SetNotifyPage(null);
            NotifyWindow = null;
        }

        private void OnAdd(object sender, EventArgs e)
        {
            var rpc = new AddFriend();
            rpc.Argument.Account = AccountEditor.Text;
            rpc.Argument.Memo = PasswordEditor.Text;
            rpc.Send(App.ClientService.GetSocket()); // skip rpc result
        }
    }
}