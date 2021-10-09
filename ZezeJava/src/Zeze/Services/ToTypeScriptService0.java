package Zeze.Services;

import Zeze.*;
import java.util.*;

public class ToTypeScriptService0 extends HandshakeClient {
	public ToTypeScriptService0(String name) {
		super(name, (getZeze().Application)null);

	}

	@Override
	public void OnSocketProcessInputBuffer(Zeze.Net.AsyncSocket so, Zeze.Serialize.ByteBuffer input) {
		if (so.isHandshakeDone()) {
			AppendInputBuffer(so.getSessionId(), input);
			input.setReadIndex(input.getWriteIndex());
		}
		else {
			super.OnSocketProcessInputBuffer(so, input);
		}
	}

	@Override
	public void OnSocketClose(Zeze.Net.AsyncSocket so, RuntimeException e) {
		SetSocketClose(so.getSessionId());
		super.OnSocketClose(so, e);
	}

	@Override
	public void OnHandshakeDone(Zeze.Net.AsyncSocket sender) {
		sender.setHandshakeDone(true);
		SetHandshakeDone(sender.getSessionId());
	}

	protected HashMap<Long, Zeze.Serialize.ByteBuffer> ToBuffer = new HashMap < Long, getZeze().Serialize.ByteBuffer>();
	protected HashSet<Long> ToHandshakeDone = new HashSet<Long>();
	protected HashSet<Long> ToSocketClose = new HashSet<Long>();

	public final void SetHandshakeDone(long socketSessionId) {
		synchronized (this) {
			ToHandshakeDone.add(socketSessionId);
		}
	}

	public final void SetSocketClose(long socketSessionId) {
		synchronized (this) {
			ToSocketClose.add(socketSessionId);
		}
	}

	public final void AppendInputBuffer(long socketSessionId, Zeze.Serialize.ByteBuffer buffer) {
		synchronized (this) {
			TValue exist;
			if (ToBuffer.containsKey(socketSessionId) && (exist = ToBuffer.get(socketSessionId)) == exist) {
				exist.Append(buffer.getBytes(), buffer.getReadIndex(), buffer.getSize());
				return;
			}
			Serialize.ByteBuffer newBuffer = Serialize.ByteBuffer.Allocate();
			ToBuffer.put(socketSessionId, newBuffer);
			newBuffer.Append(buffer.getBytes(), buffer.getReadIndex(), buffer.getSize());
		}
	}
}