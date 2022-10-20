
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

            //SecureStorage.Default.SetAsync("", "");
            // fix remove me
            // SemanticScreenReader.Announce(CounterBtn.Text);
            LoggingConfiguration();

            FollowerApplyTables.RegisterLog();
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

        private void StartApp()
        {
            if (App == null)
            {
                App = new App();
                App.Start("127.0.0.1", 5100);
                App.Zege_Friend.Bind(FriendsListView);
                App.Zege_Message.Bind(MessageView);
                FriendsListView.ItemSelected += OnFriendsItemSelected;
            }
        }

        private void OnFriendsItemSelected(object sender, EventArgs e)
        {
            var selected = FriendsListView.SelectedItem as FriendItem;
            if (null == selected)
                return;

            // TODO: make selected to top here

            App.Zege_Message.ShowHistory(selected.Account);
        }

        private MessageDrawable Drawable = new MessageDrawable();

        private double NextMessageY = 5.0;

        private void OnSendClicked(object sender, EventArgs e)
        {
            var message = MessageEditor.Text;
            message = message.Replace("\r\n", "\n");
            message = message.Replace("\r", "\n");
            if (string.IsNullOrEmpty(message))
                return;

            // test Drawable
            Drawable.Message = message;
            MessageView.Invalidate();
            // test MessageLayout

            var lastMessage = new Editor()
            {
                MinimumWidthRequest = 25,
                MaximumWidthRequest = 300,
                AutoSize = EditorAutoSizeOption.TextChanges,
                Text = message,
                BackgroundColor = Colors.LightGreen,
            };
            lastMessage.SizeChanged += OnMessageSizeChanged;
            MessageLayout.Add(lastMessage);
            //MessageLayout.SetLayoutBounds(lastMessage, new Rect(0, NextMessageY, lastMessage.Width, lastMessage.Height));
            //NextMessageY += lastMessage.Height + 5;

            App?.Zege_Message.AddMessage(message);
            MessageEditor.Text = string.Empty;
        }

        public void OnMessageSizeChanged(object sender, EventArgs e)
        {
            var lastMessage = (Editor)sender;
            MessageLayout.SetLayoutBounds(lastMessage, new Rect(0, NextMessageY, lastMessage.Width, lastMessage.Height));
            NextMessageY += lastMessage.Height + 5;
            lastMessage.SizeChanged -= OnMessageSizeChanged;
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
            MessageView.Drawable = Drawable;
            SecureStorage.Default.RemoveAll();
        }

        private void OnAddTail(object sender, EventArgs e)
        {
            App.Zege_Friend.AddNewFriend();
        }

        private void OnDeleteTail(object sender, EventArgs e)
        {
            App.Zege_Friend.DeleteTail();
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
    }
}