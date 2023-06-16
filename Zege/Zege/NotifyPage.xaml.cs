
using Zege.Friend;
using Zege.Notify;

namespace Zege;

public partial class NotifyPage : ContentPage
{
    public static NotifyPage Instance { get; private set; }

	public NotifyPage()
	{
        Instance = this;
		InitializeComponent();
        MainPage.Instance?.App?.Zege_Notify.SetNotifyPage(this);
    }

    public ListView NotifyListView => _NotifyListView;

	private BNotify GetSelectedNotify()
	{
        if (null == NotifyListView.SelectedItem)
            return null;

        var notify = NotifyListView.SelectedItem as NotifyItem;
        if (notify == null)
            return null;

        return notify.NodeValue.Value as BNotify;
    }

    private void OnAccept(object sender, EventArgs e)
	{
		var notify = GetSelectedNotify();
		if (null == notify) return;

        switch (notify.Type)
		{
			case BNotify.eTypeAddFriend:
                if (notify.Properties.TryGetValue("from", out var from))
                {
                    var rpc = new AcceptFriend();
                    rpc.Argument.Account = from;
                    rpc.Argument.Memo = Memo.Text == null ? "" : Memo.Text;
                    rpc.Send(MainPage.Instance?.App?.ClientService.GetSocket()); // skip rpc result
                }
				break;
		}
	}

    private void OnDeny(object sender, EventArgs e)
    {
        var notify = GetSelectedNotify();
        if (null == notify) return;

        switch (notify.Type)
        {
            case BNotify.eTypeAddFriend:
                if (notify.Properties.TryGetValue("from", out var from))
                {
                    var rpc = new DenyFriend();
                    rpc.Argument.Account = from;
                    rpc.Send(MainPage.Instance?.App?.ClientService.GetSocket()); // skip rpc result
                }
                break;
        }
    }
}
