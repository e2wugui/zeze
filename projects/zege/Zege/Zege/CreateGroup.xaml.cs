
using System.Text;
using Zege.Friend;
using Zege.Message;
using Zege.User;

namespace Zege
{
    public partial class CreateGroup: ContentPage
    {
        public CreateGroup()
        {
            InitializeComponent();
            FriendsListView.ItemsSource = AppShell.Instance.App.Zege_Friend.ItemsSource;
        }

        private async void OnCreateGroup(object sender, EventArgs e)
        {
            var group = await AppShell.Instance.App?.Zege_Friend.CreateGroup(SelectedAccounts);
            // todo 打开新建群的聊天窗口。
        }

        private HashSet<string> SelectedAccounts = new();
        private void OnCheckedChanged(object sender, EventArgs e)
        {
            var checkBox = (CheckBox)sender;
            if (checkBox.IsChecked)
                SelectedAccounts.Add((string)checkBox.BindingContext);
            else
                SelectedAccounts.Remove((string)checkBox.BindingContext);
        }
    }
}