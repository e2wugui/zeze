package Zeze.Net;

import Zeze.*;

public final class SocketOptions {
	// 系统选项
	private Boolean NoDelay = null;
	public Boolean getNoDelay() {
		return NoDelay;
	}
	public void setNoDelay(Boolean value) {
		NoDelay = value;
	}
	private Integer SendBuffer = null;
	public Integer getSendBuffer() {
		return SendBuffer;
	}
	public void setSendBuffer(Integer value) {
		SendBuffer = value;
	}
	private Integer ReceiveBuffer = null;
	public Integer getReceiveBuffer() {
		return ReceiveBuffer;
	}
	public void setReceiveBuffer(Integer value) {
		ReceiveBuffer = value;
	}

	// 应用选项

	// 网络层接收数据 buffer 大小，大流量网络应用需要加大。
	private int InputBufferSize;
	public int getInputBufferSize() {
		return InputBufferSize;
	}
	public void setInputBufferSize(int value) {
		InputBufferSize = value;
	}
	// 最大协议包的大小。协议需要完整收到才解析和处理，所以需要缓存。这是个安全选项。防止出现攻击占用大量内存。
	private int InputBufferMaxProtocolSize;
	public int getInputBufferMaxProtocolSize() {
		return InputBufferMaxProtocolSize;
	}
	public void setInputBufferMaxProtocolSize(int value) {
		InputBufferMaxProtocolSize = value;
	}
	private int OutputBufferMaxSize;
	public int getOutputBufferMaxSize() {
		return OutputBufferMaxSize;
	}
	public void setOutputBufferMaxSize(int value) {
		OutputBufferMaxSize = value;
	}

	// 系统选项，但没有默认，只有 ServerSocket 使用。
	private int Backlog;
	public int getBacklog() {
		return Backlog;
	}
	public void setBacklog(int value) {
		Backlog = value;
	}

	// 其他杂项
	private NLog.LogLevel SocketLogLevel;
	public NLog.LogLevel getSocketLogLevel() {
		return SocketLogLevel;
	}
	public void setSocketLogLevel(NLog.LogLevel value) {
		SocketLogLevel = value;
	}

	public SocketOptions() {
		// 这几个是应用层的选项，提供默认值。
		// 其他系统的选项不指定的话由系统提供默认值。
		setInputBufferSize(8192);
		setInputBufferMaxProtocolSize(2 * 1024 * 1024); // 2M
		setBacklog(128);

		setSocketLogLevel(NLog.LogLevel.Trace); // 可以使用 NLog.LogLevel.FromString 从配置中读取
	}
}