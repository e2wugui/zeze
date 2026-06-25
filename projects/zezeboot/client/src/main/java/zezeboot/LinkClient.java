package zezeboot;

import Zeze.Net.AsyncSocket;
import org.jetbrains.annotations.NotNull;

public class LinkClient extends LinkClientBase {
	public LinkClient(Zeze.Application zeze) {
		super(zeze);
		setNoProcedure(true);
	}

	@Override
	public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
		super.OnHandshakeDone(so);
		App.instance.zezeboot_link.onHandshakeDone(so);
	}
}
