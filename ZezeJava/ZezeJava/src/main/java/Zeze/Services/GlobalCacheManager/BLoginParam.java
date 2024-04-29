package Zeze.Services.GlobalCacheManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

public class BLoginParam implements Serializable {
	public int serverId;

	// GlobalCacheManager 本身没有编号。
	// 启用多个进程，使用 GlobalTableKey.GetHashCode() 分配负载后，报告错误需要这个来识别哪个进程。
	// 当然识别还可以根据 ServerService 绑定的ip和port。
	// 给每个实例加配置不容易维护。
	public int globalCacheManagerHashIndex;
	public boolean debugMode; // 调试模式下不检查Release Timeout,方便单步调试

	@Override
	public void decode(IByteBuffer bb) {
		serverId = bb.ReadInt();
		globalCacheManagerHashIndex = bb.ReadInt();
		debugMode = bb.ReadBool();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(serverId);
		bb.WriteInt(globalCacheManagerHashIndex);
		bb.WriteBool(debugMode);
	}

	@Override
	public int preAllocSize() {
		return 5 + 5 + 1;
	}

	@Override
	public String toString() {
		return "BLoginParam{serverId=" + serverId + ",globalCacheManagerHashIndex=" + globalCacheManagerHashIndex
				+ ",debugMode=" + debugMode + '}';
	}
}
