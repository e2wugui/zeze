package Zeze.Services.GlobalCacheManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public class BLoginParam extends Bean {
	public int ServerId;

	// GlobalCacheManager 本身没有编号。
	// 启用多个进程，使用 GlobalTableKey.GetHashCode() 分配负载后，报告错误需要这个来识别哪个进程。
	// 当然识别还可以根据 ServerService 绑定的ip和port。
	// 给每个实例加配置不容易维护。
	public int GlobalCacheManagerHashIndex;
	public boolean DebugMode; // 调试模式下不检查Release Timeout,方便单步调试

	@Override
	public void decode(ByteBuffer bb) {
		ServerId = bb.ReadInt();
		GlobalCacheManagerHashIndex = bb.ReadInt();
		DebugMode = bb.ReadBool();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(ServerId);
		bb.WriteInt(GlobalCacheManagerHashIndex);
		bb.WriteBool(DebugMode);
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void resetChildrenRootInfo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int preAllocSize() {
		return 5 + 5;
	}

	@Override
	public String toString() {
		return "BLoginParam{" + "ServerId=" + ServerId + ", GlobalCacheManagerHashIndex=" + GlobalCacheManagerHashIndex
				+ ", DebugMode=" + DebugMode + '}';
	}
}
