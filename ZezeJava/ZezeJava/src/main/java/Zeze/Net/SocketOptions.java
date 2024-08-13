package Zeze.Net;

import org.jetbrains.annotations.Nullable;

public final class SocketOptions {
	// 系统选项
	private @Nullable Boolean noDelay; // 不指定的话由系统提供默认值
	private @Nullable Integer sendBuffer; // 不指定的话由系统提供默认值
	private @Nullable Integer receiveBuffer; // 不指定的话由系统提供默认值
	private int backlog = 128; // 只有 ServerSocket 使用

	// 应用选项
	private int inputBufferMaxProtocolSize = 2 * 1024 * 1024; // 最大协议包的大小。协议需要完整收到才解析和处理，所以需要缓存。这是个安全选项。防止出现攻击占用大量内存。
	private long outputBufferMaxSize = 2 * 1024 * 1024; // 最大发送协议堆积大小. 用于Service.checkOverflow

	private @Nullable String timeThrottle;
	private @Nullable Integer timeThrottleSeconds;
	private @Nullable Integer timeThrottleLimit;
	private @Nullable Integer timeThrottleBandwidth;
	private @Nullable Long overBandwidth;
	private double overBandwidthFusingRate = 1.0;
	private double overBandwidthNormalRate = 0.7;
	private boolean closeWhenMissHandle = false;

	public boolean isCloseWhenMissHandle() {
		return closeWhenMissHandle;
	}

	public void setCloseWhenMissHandle(boolean value) {
		closeWhenMissHandle = value;
	}

	public double getOverBandwidthFusingRate() {
		return overBandwidthFusingRate;
	}

	public void setOverBandwidthFusingRate(double value) {
		overBandwidthFusingRate = value;
	}

	public double getOverBandwidthNormalRate() {
		return overBandwidthNormalRate;
	}

	public void setOverBandwidthNormalRate(double value) {
		overBandwidthNormalRate = value;
	}

	/**
	 * Service最大熔断输出带宽（字节）。当达到时会熔断（拒绝所有的请求）
	 *
	 * @return 熔断输出带宽。
	 */
	public @Nullable Long getOverBandwidth() {
		return overBandwidth;
	}

	public void setOverBandwidth(@Nullable Long value) {
		overBandwidth = value;
	}

	public @Nullable String getTimeThrottle() {
		return timeThrottle;
	}

	public void setTimeThrottle(@Nullable String name) {
		timeThrottle = name;
	}

	public @Nullable Integer getTimeThrottleBandwidth() {
		return timeThrottleBandwidth;
	}

	public void setTimeThrottleBandwidth(int band) {
		timeThrottleBandwidth = band;
	}

	public @Nullable Integer getTimeThrottleSeconds() {
		return timeThrottleSeconds;
	}

	public void setTimeThrottleSeconds(int seconds) {
		timeThrottleSeconds = seconds;
	}

	public @Nullable Integer getTimeThrottleLimit() {
		return timeThrottleLimit;
	}

	public void setTimeThrottleLimit(int limit) {
		timeThrottleLimit = limit;
	}

	public @Nullable Boolean getNoDelay() {
		return noDelay;
	}

	public void setNoDelay(@Nullable Boolean value) {
		noDelay = value;
	}

	public @Nullable Integer getSendBuffer() {
		return sendBuffer;
	}

	public void setSendBuffer(@Nullable Integer value) {
		sendBuffer = value;
	}

	public @Nullable Integer getReceiveBuffer() {
		return receiveBuffer;
	}

	public void setReceiveBuffer(@Nullable Integer value) {
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
