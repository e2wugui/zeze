package Zeze.Raft;

/**
 * Raft 应用 Rpc 的必备接口：唯一请求标识（服务端去重依据）与创建/发送时间。
 */
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
