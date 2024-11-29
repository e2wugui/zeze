package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.IntList;
import Zeze.Util.LongList;

public class BAnnounceServers implements Serializable {
	private int version; // 协议版本. 目前固定为0,为以后可能扩展或修改字段用
	public String notifyId; // 通知ID. 不同的通知ID之间互不影响
	public int serverId; // 自己的serverId
	public final IntList watchServerIds = new IntList(); // 需要关注的所有serverIds,超过一定时间没连上ServiceManager就会得到offline通知
	public final LongList watchSerialIds = new LongList(); // 对应watchServerIds的各个LoadSerialNo

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(version);
		bb.WriteString(notifyId);
		bb.WriteInt(serverId);
		{
			int n = watchServerIds.size();
			bb.WriteUInt(n);
			for (int i = 0; i < n; i++)
				bb.WriteInt(watchServerIds.get(i));
		}
		{
			int n = watchSerialIds.size();
			bb.WriteUInt(n);
			for (int i = 0; i < n; i++)
				bb.WriteLong(watchSerialIds.get(i));
		}
	}

	@Override
	public void decode(IByteBuffer bb) {
		version = bb.ReadInt();
		notifyId = bb.ReadString();
		serverId = bb.ReadInt();
		{
			int n = bb.ReadUInt();
			watchServerIds.clear();
			watchServerIds.reserve(n);
			for (int i = 0; i < n; i++)
				watchServerIds.add(bb.ReadInt());
		}
		{
			int n = bb.ReadUInt();
			watchSerialIds.clear();
			watchSerialIds.reserve(n);
			for (int i = 0; i < n; i++)
				watchSerialIds.add(bb.ReadLong());
		}
	}

	private static int _PRE_ALLOC_SIZE_ = 16;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}

	@Override
	public String toString() {
		return "BAnnounceServers{version=" + version
				+ ",notifyId=" + notifyId
				+ ",serverId=" + serverId
				+ ",watchServerIds=" + watchServerIds.dump()
				+ ",watchSerialIds=" + watchSerialIds.dump()
				+ '}';
	}
}
