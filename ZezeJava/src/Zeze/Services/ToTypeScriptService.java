package Zeze.Services;

import Zeze.*;
import java.util.*;

public class ToTypeScriptService extends Zeze.Services.ToTypeScriptService0 {
	public CallbackOnSocketHandshakeDone CallbackWhenSocketHandshakeDone;
	public CallbackOnSocketClose CallbackWhenSocketClose;
	public CallbackOnSocketProcessInputBuffer CallbackWhenSocketProcessInputBuffer;

	public ToTypeScriptService(String name) {
		super(name);
	}


	public final void Connect(String hostNameOrAddress, int port) {
		Connect(hostNameOrAddress, port, true);
	}

//C# TO JAVA CONVERTER WARNING: There is no Java equivalent to C#'s shadowing via the 'new' keyword:
//ORIGINAL LINE: public new void Connect(string hostNameOrAddress, int port, bool autoReconnect = true)
//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
	public final void Connect(String hostNameOrAddress, int port, boolean autoReconnect) {
		super.Connect(hostNameOrAddress, port, autoReconnect);
	}

	public final void Send(long sessionId, Puerts.ArrayBuffer buffer, int offset, int len) {
		if (super_Keyword.GetSocket(sessionId) != null) {
			super_Keyword.GetSocket(sessionId).Send(buffer.Bytes, offset, len);
		}
	}

	public final void Close(long sessionId) {
		if (super_Keyword.GetSocket(sessionId) != null) {
			super_Keyword.GetSocket(sessionId).Dispose();
		}
	}

	public final void TickUpdate() {
		HashSet<Long> handshakeTmp;
		HashSet<Long> socketCloseTmp;
		HashMap<Long, Zeze.Serialize.ByteBuffer> inputTmp;
		synchronized (this) {
			handshakeTmp = ToHandshakeDone;
			socketCloseTmp = ToSocketClose;
			inputTmp = ToBuffer;

			ToBuffer = new HashMap < Long, getZeze().Serialize.ByteBuffer>();
			ToHandshakeDone = new HashSet<Long>();
			ToSocketClose = new HashSet<Long>();
		}

		for (var e : socketCloseTmp) {
			this.CallbackWhenSocketClose.invoke(e);
		}

		for (var e : handshakeTmp) {
			this.CallbackWhenSocketHandshakeDone.invoke(e);
		}

		for (var e : inputTmp.entrySet()) {
			this.CallbackWhenSocketProcessInputBuffer.invoke(e.getKey(), new Puerts.ArrayBuffer(e.getValue().Bytes), e.getValue().ReadIndex, e.getValue().Size);
		}
	}
}
//#endif
