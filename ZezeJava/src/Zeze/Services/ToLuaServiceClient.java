package Zeze.Services;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.*;
import java.util.*;

public class ToLuaServiceClient extends HandshakeClient implements FromLua {
	private Zeze.Services.ToLuaService.ToLua ToLua = new Zeze.Services.ToLuaService.ToLua();
	public final Zeze.Services.ToLuaService.ToLua getToLua() {
		return ToLua;
	}
	private void setToLua(Zeze.Services.ToLuaService.ToLua value) {
		ToLua = value;
	}
	public final Net.Service getService() {
		return this;
	}

	public ToLuaServiceClient(String name, Application app) {
		super(name, app);
	}

	public final void InitializeLua(Zeze.Services.ToLuaService.ILua iLua) {
		getToLua().InitializeLua(iLua);
		getToLua().RegisterGlobalAndCallback(this);
	}

	@Override
	public void OnHandshakeDone(AsyncSocket sender) {
		sender.setHandshakeDone(true);
		getToLua().SetHandshakeDone(sender.getSessionId(), this);
	}

	@Override
	public void OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input) {
		if (so.isHandshakeDone()) {
			getToLua().AppendInputBuffer(so.getSessionId(), input);
			input.setReadIndex(input.getWriteIndex());
		}
		else {
			super.OnSocketProcessInputBuffer(so, input);
		}
	}

	@Override
	public void OnSocketClose(AsyncSocket so, RuntimeException e) {
		getToLua().SetSocketClose(so.getSessionId(), this);
		super.OnSocketClose(so, e);
	}
}