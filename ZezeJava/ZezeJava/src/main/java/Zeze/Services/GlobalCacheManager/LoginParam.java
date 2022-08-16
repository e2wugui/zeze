package Zeze.Services.GlobalCacheManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public class LoginParam extends Bean {
	public int ServerId;

	// GlobalCacheManager 本身没有编号。
	// 启用多个进程，使用 GlobalTableKey.GetHashCode() 分配负载后，报告错误需要这个来识别哪个进程。
	// 当然识别还可以根据 ServerService 绑定的ip和port。
	// 给每个实例加配置不容易维护。
	public int GlobalCacheManagerHashIndex;

	@Override
	public void Decode(ByteBuffer bb) {
		ServerId = bb.ReadInt();
		GlobalCacheManagerHashIndex = bb.ReadInt();
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteInt(ServerId);
		bb.WriteInt(GlobalCacheManagerHashIndex);
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void ResetChildrenRootInfo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int preAllocSize() {
		return 5 + 5;
	}

	@Override
	public String toString() {
		return "LoginParam{" + "ServerId=" + ServerId + ", GlobalCacheManagerHashIndex=" + GlobalCacheManagerHashIndex + '}';
	}
}
