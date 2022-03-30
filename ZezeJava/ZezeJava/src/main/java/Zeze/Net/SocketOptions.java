package Zeze.Net;

import org.apache.logging.log4j.Level;

public final class SocketOptions {
	// 系统选项
	private Boolean NoDelay; // 不指定的话由系统提供默认值
	private Integer SendBuffer; // 不指定的话由系统提供默认值
	private Integer ReceiveBuffer; // 不指定的话由系统提供默认值
	private int Backlog = 128; // 只有 ServerSocket 使用

	// 应用选项
	private int InputBufferSize = 8192; // 网络层接收数据 buffer 大小，大流量网络应用需要加大。
	private int InputBufferMaxProtocolSize = 2 * 1024 * 1024; // 最大协议包的大小。协议需要完整收到才解析和处理，所以需要缓存。这是个安全选项。防止出现攻击占用大量内存。
	private int OutputBufferMaxSize = 2 * 1024 * 1024;

	// 其他杂项
	private Level SocketLogLevel = Level.TRACE;

	public Boolean getNoDelay() {
		return NoDelay;
	}

	public void setNoDelay(Boolean value) {
		NoDelay = value;
	}

	public Integer getSendBuffer() {
		return SendBuffer;
	}

	public void setSendBuffer(Integer value) {
		SendBuffer = value;
	}

	public Integer getReceiveBuffer() {
		return ReceiveBuffer;
	}

	public void setReceiveBuffer(Integer value) {
		ReceiveBuffer = value;
	}

	public int getBacklog() {
		return Backlog;
	}

	public void setBacklog(int value) {
		Backlog = value;
	}

	public int getInputBufferSize() {
		return InputBufferSize;
	}

	public void setInputBufferSize(int value) {
		InputBufferSize = value;
	}

	public int getInputBufferMaxProtocolSize() {
		return InputBufferMaxProtocolSize;
	}

	public void setInputBufferMaxProtocolSize(int value) {
		InputBufferMaxProtocolSize = value;
	}

	public int getOutputBufferMaxSize() {
		return OutputBufferMaxSize;
	}

	public void setOutputBufferMaxSize(int value) {
		OutputBufferMaxSize = value;
	}

	public Level getSocketLogLevel() {
		return SocketLogLevel;
	}

	public void setSocketLogLevel(Level value) {
		SocketLogLevel = value;
	}
}
