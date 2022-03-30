package Zeze.Raft;

public interface IRaftRpc {
	long getCreateTime();

	void setCreateTime(long value);

	/**
	 * 唯一的请求编号，重发时保持不变。在一个ClientId内唯一即可。
	 */
	UniqueRequestId getUnique();

	void setUnique(UniqueRequestId value);

	/**
	 * 不序列化，Agent本地只用。
	 */
	long getSendTime();

	void setSendTime(long value);
}
