package Zeze.Net;

public final class SocketOptions {
	// 系统选项
	private Boolean noDelay; // 不指定的话由系统提供默认值
	private Integer sendBuffer; // 不指定的话由系统提供默认值
	private Integer receiveBuffer; // 不指定的话由系统提供默认值
	private int backlog = 128; // 只有 ServerSocket 使用

	// 应用选项
	private int inputBufferMaxProtocolSize = 2 * 1024 * 1024; // 最大协议包的大小。协议需要完整收到才解析和处理，所以需要缓存。这是个安全选项。防止出现攻击占用大量内存。
	private long outputBufferMaxSize = 2 * 1024 * 1024; // 最大发送协议堆积大小. 用于Service.checkOverflow

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

	public int getInputBufferMaxProtocolSize() {
		return inputBufferMaxProtocolSize;
	}

	public void setInputBufferMaxProtocolSize(int value) {
		inputBufferMaxProtocolSize = value;
	}

	public long getOutputBufferMaxSize() {
		return outputBufferMaxSize;
	}

	public void setOutputBufferMaxSize(long value) {
		outputBufferMaxSize = value;
	}
}
