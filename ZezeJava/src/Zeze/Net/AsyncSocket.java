package Zeze.Net;

import Zeze.Serialize.*;
import Zeze.*;
import java.util.*;
import java.io.*;

/** 
 使用Socket的BeginXXX,EndXXX XXXAsync方法的异步包装类。
 目前只支持Tcp。
*/
public final class AsyncSocket implements Closeable {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private byte[] _inputBuffer;
	private byte[] _inputBuffer;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private List<System.ArraySegment<byte>> _outputBufferList = null;
	private ArrayList<System.ArraySegment<Byte>> _outputBufferList = null;
	private int _outputBufferListCountSum = 0;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private List<System.ArraySegment<byte>> _outputBufferListSending = null;
	private ArrayList<System.ArraySegment<Byte>> _outputBufferListSending = null; // 正在发送的 buffers.
	private int _outputBufferListSendingCountSum = 0;
	private Service Service;
	public Service getService() {
		return Service;
	}
	private void setService(Service value) {
		Service = value;
	}
	private Connector Connector;
	public Connector getConnector() {
		return Connector;
	}
	public void setConnector(Connector value) {
		Connector = value;
	}
	private Acceptor Acceptor;
	public Acceptor getAcceptor() {
		return Acceptor;
	}
	public void setAcceptor(Acceptor value) {
		Acceptor = value;
	}

	private RuntimeException LastException;
	public RuntimeException getLastException() {
		return LastException;
	}
	private void setLastException(RuntimeException value) {
		LastException = value;
	}
	private long SessionId;
	public long getSessionId() {
		return SessionId;
	}
	private void setSessionId(long value) {
		SessionId = value;
	}
	private Socket Socket;
	public Socket getSocket() {
		return Socket;
	}
	private void setSocket(Socket value) {
		Socket = value;
	}

	/** 
	 保存需要存储在Socket中的状态。
	 简单变量，没有考虑线程安全问题。
	 内部不使用。
	*/
	private Object UserState;
	public Object getUserState() {
		return UserState;
	}
	public void setUserState(Object value) {
		UserState = value;
	}
	private boolean IsHandshakeDone;
	public boolean isHandshakeDone() {
		return IsHandshakeDone;
	}
	public void setHandshakeDone(boolean value) {
		IsHandshakeDone = value;
	}

	private static Zeze.Util.AtomicLong SessionIdGen = new Zeze.Util.AtomicLong();

	private SocketAsyncEventArgs eventArgsAccept;
	private SocketAsyncEventArgs eventArgsReceive;
	private SocketAsyncEventArgs eventArgsSend;

	private BufferCodec inputCodecBuffer = new BufferCodec(); // 记录这个变量用来操作buffer
	private BufferCodec outputCodecBuffer = new BufferCodec(); // 记录这个变量用来操作buffer

	private Codec inputCodecChain;
	private Codec outputCodecChain;

	private String RemoteAddress;
	public String getRemoteAddress() {
		return RemoteAddress;
	}
	private void setRemoteAddress(String value) {
		RemoteAddress = value;
	}

	/** 
	 for server socket
	*/
	public AsyncSocket(Service service, System.Net.EndPoint localEP) {
		this.setService(service);

		setSocket(new Socket(SocketType.Stream, ProtocolType.Tcp));
		getSocket().Blocking = false;
		getSocket().SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);

		// xxx 只能设置到 ServerSocket 中，以后 Accept 的连接通过继承机制得到这个配置。
		// 不知道 c# 会不会也这样，先这样写。
		if (null != service.SocketOptions.ReceiveBuffer) {
			getSocket().ReceiveBufferSize = service.SocketOptions.ReceiveBuffer.Value;
		}

		getSocket().Bind(localEP);
		getSocket().Listen(service.SocketOptions.Backlog);

		this.setSessionId(SessionIdGen.IncrementAndGet());

		eventArgsAccept = new SocketAsyncEventArgs();
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C#-style event wireups:
		eventArgsAccept.Completed += OnAsyncIOCompleted;

		BeginAcceptAsync();
	}

	/** 
	 use inner. create when accept;
	 
	 @param accepted
	*/
	private AsyncSocket(Service service, Socket accepted) {
		this.setService(service);

		setSocket(accepted);
		getSocket().Blocking = false;

		// 据说连接接受以后设置无效，应该从 ServerSocket 继承
		if (null != service.SocketOptions.ReceiveBuffer) {
			getSocket().ReceiveBufferSize = service.SocketOptions.ReceiveBuffer.Value;
		}
		if (null != service.SocketOptions.SendBuffer) {
			getSocket().SendBufferSize = service.SocketOptions.SendBuffer.Value;
		}
		if (null != service.SocketOptions.NoDelay) {
			getSocket().NoDelay = service.SocketOptions.NoDelay.Value;
		}

		this.setSessionId(SessionIdGen.IncrementAndGet());

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: this._inputBuffer = new byte[service.SocketOptions.InputBufferSize];
		this._inputBuffer = new byte[service.SocketOptions.InputBufferSize];

		System.Net.EndPoint tempVar = getSocket().RemoteEndPoint;
		var remoteIp = tempVar instanceof IPEndPoint ? (IPEndPoint)tempVar : null;
		setRemoteAddress(remoteIp.Address.GetAddressBytes().toString());

		BeginReceiveAsync();
	}

	/** 
	 for client socket. connect
	 
	 @param hostNameOrAddress
	 @param port
	*/

	public AsyncSocket(Service service, String hostNameOrAddress, int port) {
		this(service, hostNameOrAddress, port, null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public AsyncSocket(Service service, string hostNameOrAddress, int port, object userState = null)
	public AsyncSocket(Service service, String hostNameOrAddress, int port, Object userState) {
		this.setService(service);

		setSocket(new Socket(SocketType.Stream, ProtocolType.Tcp));
		getSocket().Blocking = false;
		setUserState(userState);

		if (null != service.SocketOptions.ReceiveBuffer) {
			getSocket().ReceiveBufferSize = service.SocketOptions.ReceiveBuffer.Value;
		}
		if (null != service.SocketOptions.SendBuffer) {
			getSocket().SendBufferSize = service.SocketOptions.SendBuffer.Value;
		}
		if (null != service.SocketOptions.NoDelay) {
			getSocket().NoDelay = service.SocketOptions.NoDelay.Value;
		}

		this.setSessionId(SessionIdGen.IncrementAndGet());

		System.Net.Dns.BeginGetHostAddresses(hostNameOrAddress, ::OnAsyncGetHostAddresses, port);
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void SetOutputSecurityCodec(byte[] key, bool compress)
	public void SetOutputSecurityCodec(byte[] key, boolean compress) {
		synchronized (this) {
			Codec chain = outputCodecBuffer;
			if (null != key) {
				chain = new Encrypt(chain, key);
			}
			if (compress) {
				chain = new Compress(chain);
			}
			if (outputCodecChain != null) {
				outputCodecChain.close();
			}
			outputCodecChain = chain;
			setOutputSecurity(true);
		}
	}

	private boolean IsInputSecurity;
	public boolean isInputSecurity() {
		return IsInputSecurity;
	}
	private void setInputSecurity(boolean value) {
		IsInputSecurity = value;
	}
	private boolean IsOutputSecurity;
	public boolean isOutputSecurity() {
		return IsOutputSecurity;
	}
	private void setOutputSecurity(boolean value) {
		IsOutputSecurity = value;
	}
	public boolean isSecurity() {
		return isInputSecurity() && isOutputSecurity();
	}

	public void VerifySecurity() {
		if (!isSecurity()) {
			throw new RuntimeException(String.format("%1$s !IsSecurity", getService().getName()));
		}
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void SetInputSecurityCodec(byte[] key, bool compress)
	public void SetInputSecurityCodec(byte[] key, boolean compress) {
		synchronized (this) {
			Codec chain = inputCodecBuffer;
			if (compress) {
				chain = new Decompress(chain);
			}
			if (null != key) {
				chain = new Decrypt(chain, key);
			}
			if (inputCodecChain != null) {
				inputCodecChain.close();
			}
			inputCodecChain = chain;
			setInputSecurity(true);
		}
	}

	public boolean Send(Protocol protocol) {
		return Send(protocol.Encode());
	}

	public boolean Send(Zeze.Serialize.ByteBuffer bb) {
		return Send(bb.getBytes(), bb.getReadIndex(), bb.getSize());
	}

	public boolean Send(Binary binary) {
		return Send(binary.getBytes(), binary.getOffset(), binary.getCount());
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public bool Send(byte[] bytes)
	public boolean Send(byte[] bytes) {
		return Send(bytes, 0, bytes.length);
	}

	/** 
	 可能直接加到发送缓冲区，不能再修改bytes了。
	 
	 @param bytes
	 @param offset
	 @param length
	*/
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public bool Send(byte[] bytes, int offset, int length)
	public boolean Send(byte[] bytes, int offset, int length) {
		Zeze.Serialize.ByteBuffer.VerifyArrayIndex(bytes, offset, length);

		synchronized (this) {
			if (null == getSocket()) {
				return false;
			}

			if (null != outputCodecChain) {
				// 压缩加密等 codec 链操作。
				outputCodecBuffer.getBuffer().EnsureWrite(length); // reserve
				outputCodecChain.update(bytes, offset, length);
				outputCodecChain.flush();

				// 修改参数，后面继续使用处理过的数据继续发送。
				bytes = outputCodecBuffer.getBuffer().getBytes();
				offset = outputCodecBuffer.getBuffer().getReadIndex();
				length = outputCodecBuffer.getBuffer().getSize();

				// outputBufferCodec 释放对byte[]的引用。
				outputCodecBuffer.getBuffer().FreeInternalBuffer();
			}

			if (null == _outputBufferList) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: _outputBufferList = new List<ArraySegment<byte>>();
				_outputBufferList = new ArrayList<ArraySegment<Byte>>();
			}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: _outputBufferList.Add(new ArraySegment<byte>(bytes, offset, length));
			_outputBufferList.add(new ArraySegment<Byte>(bytes, offset, length));
			_outputBufferListCountSum += _outputBufferList.get(_outputBufferList.size() - 1).Count;

			if (null == _outputBufferListSending) { // 没有在发送中，马上请求发送，否则等回调处理。
				_outputBufferListSending = _outputBufferList;
				_outputBufferList = null;
				_outputBufferListSendingCountSum = _outputBufferListCountSum;
				_outputBufferListCountSum = 0;

				if (null == eventArgsSend) {
					eventArgsSend = new SocketAsyncEventArgs();
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C#-style event wireups:
					eventArgsSend.Completed += OnAsyncIOCompleted;
				}
				eventArgsSend.BufferList = _outputBufferListSending;
				if (false == getSocket().SendAsync(eventArgsSend)) {
					ProcessSend(eventArgsSend);
				}
			}
			return true;
		}
	}

	public boolean Send(String str) {
		return Send(str.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	private void OnAsyncIOCompleted(Object sender, SocketAsyncEventArgs e) {
		if (getSocket() == null) { // async closed
			return;
		}

		try {
			switch (e.LastOperation) {
				case Accept:
					ProcessAccept(e);
					break;
				case Send:
					ProcessSend(e);
					break;
				case Receive:
					ProcessReceive(e);
					break;
				default:
					throw new IllegalArgumentException();
			}
		}
		catch (RuntimeException ex) {
			Close(ex);
		}
	}

	private void BeginAcceptAsync() {
		eventArgsAccept.AcceptSocket = null;
		if (false == getSocket().AcceptAsync(eventArgsAccept)) {
			ProcessAccept(eventArgsAccept);
		}
	}

	private void ProcessAccept(SocketAsyncEventArgs e) {
		if (e.SocketError == SocketError.Success) {
			AsyncSocket accepted = null;
			try {
				accepted = new AsyncSocket(this.getService(), e.AcceptSocket);
				accepted.setAcceptor(this.getAcceptor());
				this.getService().OnSocketAccept(accepted);
			}
			catch (RuntimeException ce) {
				if (accepted != null) {
					accepted.Close(ce);
				}
			}
			BeginAcceptAsync();
		}
		/*
		else
		{
		    Console.WriteLine("ProcessAccept " + e.SocketError);
		}
		*/
	}

	private void OnAsyncGetHostAddresses(IAsyncResult ar) {
		try {
			int port = (Integer)ar.AsyncState;
			System.Net.IPAddress[] addrs = System.Net.Dns.EndGetHostAddresses(ar);
			getSocket().BeginConnect(addrs, port, ::OnAsyncConnect, this);
		}
		catch (RuntimeException e) {
			this.getService().OnSocketConnectError(this, e);
			Close(null);
		}
	}

	private void OnAsyncConnect(IAsyncResult ar) {
		try {
			this.getSocket().EndConnect(ar);
			if (this.getConnector() != null) {
				this.getConnector().OnSocketConnected(this);
			}
			this.getService().OnSocketConnected(this);

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: this._inputBuffer = new byte[Service.SocketOptions.InputBufferSize];
			this._inputBuffer = new byte[getService().SocketOptions.InputBufferSize];
			BeginReceiveAsync();
		}
		catch (RuntimeException e) {
			this.getService().OnSocketConnectError(this, e);
			Close(null);
		}
	}

	private void BeginReceiveAsync() {
		if (null == eventArgsReceive) {
			eventArgsReceive = new SocketAsyncEventArgs();
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C#-style event wireups:
			eventArgsReceive.Completed += OnAsyncIOCompleted;
		}

		eventArgsReceive.SetBuffer(_inputBuffer, 0, _inputBuffer.length);
		if (false == this.getSocket().ReceiveAsync(eventArgsReceive)) {
			ProcessReceive(eventArgsReceive);
		}
	}

	private void ProcessReceive(SocketAsyncEventArgs e) {
		if (e.BytesTransferred > 0 && e.SocketError == SocketError.Success) {
			if (null != inputCodecChain) {
				// 解密解压处理，处理结果直接加入 inputCodecBuffer。
				inputCodecBuffer.getBuffer().EnsureWrite(e.BytesTransferred);
				inputCodecChain.update(_inputBuffer, 0, e.BytesTransferred);
				inputCodecChain.flush();

				this.getService().OnSocketProcessInputBuffer(this, inputCodecBuffer.getBuffer());
			}
			else if (inputCodecBuffer.getBuffer().getSize() > 0) {
				// 上次解析有剩余数据（不完整的协议），把新数据加入。
				inputCodecBuffer.getBuffer().Append(_inputBuffer, 0, e.BytesTransferred);

				this.getService().OnSocketProcessInputBuffer(this, inputCodecBuffer.getBuffer());
			}
			else {
				ByteBuffer avoidCopy = ByteBuffer.Wrap(_inputBuffer, 0, e.BytesTransferred);

				this.getService().OnSocketProcessInputBuffer(this, avoidCopy);

				if (avoidCopy.getSize() > 0) { // 有剩余数据（不完整的协议），加入 inputCodecBuffer 等待新的数据。
					inputCodecBuffer.getBuffer().Append(avoidCopy.getBytes(), avoidCopy.getReadIndex(), avoidCopy.getSize());
				}
			}

			// 1 检测 buffer 是否满，2 剩余数据 Campact，3 需要的话，释放buffer内存。
			int remain = inputCodecBuffer.getBuffer().getSize();
			if (remain > 0) {
				if (remain >= getService().SocketOptions.InputBufferMaxProtocolSize) {
					throw new RuntimeException("InputBufferMaxProtocolSize " + getService().SocketOptions.InputBufferMaxProtocolSize);
				}

				inputCodecBuffer.getBuffer().Campact();
			}
			else {
				inputCodecBuffer.getBuffer().FreeInternalBuffer(); // 解析缓冲如果为空，马上释放内部bytes[]。
			}

			BeginReceiveAsync();
		}
		else {
			Close(null); // 正常关闭，不设置异常
		}
	}

	private void ProcessSend(SocketAsyncEventArgs e) {
		if (e.BytesTransferred >= 0 && e.SocketError == SocketError.Success) {
			BeginSendAsync(e.BytesTransferred);
		}
		else {
			Close(new SocketException(e.SocketError.getValue()));
		}
	}

	private void BeginSendAsync(int _bytesTransferred) {
		synchronized (this) {
			// 听说 BeginSend 成功回调的时候，所有数据都会被发送，这样的话就可以直接清除_outputBufferSending，而不用这么麻烦。
			// MUST 下面的条件必须满足，不做判断。
			// _outputBufferSending != null
			// _outputBufferSending.Count > 0
			// sum(_outputBufferSending[i].Count) <= bytesTransferred
			int bytesTransferred = _bytesTransferred; // 后面还要用已经发送的原始值，本来下面计算也可以得到，但这样更容易理解。
			if (bytesTransferred == _outputBufferListSendingCountSum) { // 全部发送完，优化。
				_outputBufferListSending.clear();
			}
			else if (bytesTransferred > _outputBufferListSendingCountSum) {
				throw new RuntimeException("hasSend too big.");
			}
			else {
				// 部分发送
				for (int i = 0; i < _outputBufferListSending.size(); ++i) {
					int bytesCount = _outputBufferListSending.get(i).Count;
					if (bytesTransferred >= bytesCount) {
						bytesTransferred -= bytesCount;
						if (bytesTransferred > 0) {
							continue;
						}

						_outputBufferListSending.subList(0, i + 1).clear();
						break;
					}
					// 已经发送的数据比数组中的少。
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: ArraySegment<byte> segment = _outputBufferListSending[i];
					ArraySegment<Byte> segment = _outputBufferListSending.get(i);
					// Slice .net framework 没有定义。
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: _outputBufferListSending[i] = new ArraySegment<byte>(segment.Array, bytesTransferred, segment.Count - bytesTransferred);
					_outputBufferListSending.set(i, new ArraySegment<Byte>(segment.Array, bytesTransferred, segment.Count - bytesTransferred));
					_outputBufferListSending.subList(0, i).clear();
					break;
				}
			}

			if (_outputBufferListSending.isEmpty()) {
				// 全部发送完
				_outputBufferListSending = _outputBufferList; // maybe null
				_outputBufferList = null;
				_outputBufferListSendingCountSum = _outputBufferListCountSum;
				_outputBufferListCountSum = 0;
			}
			else if (null != _outputBufferList) {
				// 没有发送完，并且有要发送的
				_outputBufferListSending.addAll(_outputBufferList);
				_outputBufferList = null;
				_outputBufferListSendingCountSum = _outputBufferListCountSum + (_outputBufferListSendingCountSum - _bytesTransferred);
				_outputBufferListCountSum = 0;
			}
			else {
				// 没有发送完，也没有要发送的
				_outputBufferListSendingCountSum = _outputBufferListSendingCountSum - _bytesTransferred;
			}

			if (null != _outputBufferListSending) { // 全部发送完，并且 _outputBufferList == null 时，可能为 null
				eventArgsSend.BufferList = _outputBufferListSending;
				if (false == getSocket().SendAsync(eventArgsSend)) {
					ProcessSend(eventArgsSend);
				}
			}
		}
	}

	public void Close(RuntimeException e) {
		this.setLastException(e);
		close();
	}

	public void close() throws IOException {
		synchronized (this) {
			if (getSocket() == null) {
				return;
			}

			try {
				if (getConnector() != null) {
					getConnector().OnSocketClose(this);
				}
				getService().OnSocketClose(this, this.getLastException());
				if (getSocket() != null) {
					getSocket().Dispose();
				}
				setSocket(null);
			}
			catch (RuntimeException e) {
				// skip Dispose error
			}
		}

		synchronized (this) {
			try {
				getService().OnSocketDisposed(this);
			}
			catch (RuntimeException e2) {
				// skip Dispose error
			}
		}
	}

	public void SetSessionId(long newSessionId) {
		if (getService().SocketMapInternal.TryRemove(KeyValuePair.Create(getSessionId(), this))) {
			if (!getService().SocketMapInternal.TryAdd(newSessionId, this)) {
				getService().SocketMapInternal.TryAdd(getSessionId(), this); // rollback
				throw new RuntimeException(String.format("duplicate sessionid %1$s", this));
			}
			setSessionId(newSessionId);
		}
		else {
			// 为了简化并发问题，只能加入Service以后的Socket的SessionId。
			throw new RuntimeException(String.format("Not Exist In Service %1$s", this));
		}
	}
}