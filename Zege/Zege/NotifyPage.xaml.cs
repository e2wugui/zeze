
using System.Buffers.Text;
using Zege.Friend;
using Zege.Notify;
using Zeze.Util;

namespace Zege;

public partial class NotifyPage : ContentPage
{
    public static NotifyPage Instance { get; private set; }

	public NotifyPage()
	{
        Instance = this;
		InitializeComponent();
        AppShell.Instance.App?.Zege_Notify.SetNotifyPage(this);
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

    private async void OnAccept(object sender, EventArgs e)
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
                    rpc.Send(AppShell.Instance.App?.ClientService.GetSocket()); // skip rpc result
                }
                break;

            case BNotify.eTypeGroupCert:
                if (notify.Properties.TryGetValue("group", out var group) && notify.Properties.TryGetValue("lastCertIndex", out var lastCertIndex))
                {
                    var cert = await AppShell.Instance.App.Zege_User.GetMyPrivateCertificate();
                    if (cert != null)
                    {
                        var pkcs12 = Cert.DecryptAesWithRsa(cert, notify.Data.GetBytesUnsafe(), notify.Data.Offset, notify.Data.Count);
                        var base64 = Convert.ToBase64String(pkcs12);
                        await SecureStorage.Default.SetAsync(group + "." + lastCertIndex + ".pkcs12", base64);
                        await SecureStorage.Default.SetAsync(group + ".LastCertIndex", lastCertIndex.ToString());
                    }
                    var r = new RemoveNotify();
                    r.Argument.Account = group;
                    r.Argument.Type = BNotify.eTypeGroupCert;
                    await r.SendAsync(AppShell.Instance.App.Connector.TryGetReadySocket());
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
                    rpc.Send(AppShell.Instance.App?.ClientService.GetSocket()); // skip rpc result
                }
                break;
        }
    }
}
