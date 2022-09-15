package Zeze.Net;

import org.apache.logging.log4j.Level;

public final class SocketOptions {
	// 系统选项
	private Boolean noDelay; // 不指定的话由系统提供默认值
	private Integer sendBuffer; // 不指定的话由系统提供默认值
	private Integer receiveBuffer; // 不指定的话由系统提供默认值
	private int backlog = 128; // 只有 ServerSocket 使用

	// 应用选项
	private int inputBufferSize = 8192; // 网络层接收数据 buffer 大小，大流量网络应用需要加大。
	private int inputBufferMaxProtocolSize = 2 * 1024 * 1024; // 最大协议包的大小。协议需要完整收到才解析和处理，所以需要缓存。这是个安全选项。防止出现攻击占用大量内存。
	private int outputBufferMaxSize = 2 * 1024 * 1024;

	// 其他杂项
	private Level socketLogLevel = Level.TRACE;

	public Boolean getNoDelay() {
		return noDelay;
	}

	public void setNoDelay(Boolean value) {
		noDelay = value;
	}

	public Integer getSendBuffer() {
		return sendBuffer;
	}

	public void setSendBuffer(Integer value) {
		sendBuffer = value;
	}

	public Integer getReceiveBuffer() {
		return receiveBuffer;
	}

	public void setReceiveBuffer(Integer value) {
		receiveBuffer = value;
	}

	public int getBacklog() {
		return backlog;
	}

	public void setBacklog(int value) {
		backlog = value;
	}

	public int getInputBufferSize() {
		return inputBufferSize;
	}

	public void setInputBufferSize(int value) {
		inputBufferSize = value;
	}

	public int getInputBufferMaxProtocolSize() {
		return inputBufferMaxProtocolSize;
	}

	public void setInputBufferMaxProtocolSize(int value) {
		inputBufferMaxProtocolSize = value;
	}

	public int getOutputBufferMaxSize() {
		return outputBufferMaxSize;
	}

	public void setOutputBufferMaxSize(int value) {
		outputBufferMaxSize = value;
	}

	public Level getSocketLogLevel() {
		return socketLogLevel;
	}

	public void setSocketLogLevel(Level value) {
		socketLogLevel = value;
	}
}
