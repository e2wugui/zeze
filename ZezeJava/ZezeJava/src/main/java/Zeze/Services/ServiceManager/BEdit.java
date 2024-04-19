package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class BEdit extends Bean {
	public final @NotNull List<BServiceInfo> remove = new ArrayList<>(); // 注销，删除，忽略不存在的。
	public final @NotNull List<BServiceInfo> put = new ArrayList<>(); // 注册，增加或替换。
	public final @NotNull List<BServiceInfo> update = new ArrayList<>(); // 更新，忽略不存在的，只更新局部信息。

	// 处理顺序：remove,put,update。
	// 当不同的集合中存在相同的服务时，要注意这个处理顺序。
	// 也就是说 put 等级更高，服务优先能找到。
	// update处理是特殊的，放在put后面，使得可以一次注册同时马上更新。

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteUInt(remove.size());
		for (var r : remove)
			r.encode(bb);
		bb.WriteUInt(put.size());
		for (var p : put)
			p.encode(bb);
		bb.WriteUInt(update.size());
		for (var u : update)
			u.encode(bb);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		for (var i = bb.ReadUInt(); i > 0; --i) {
			var r = new BServiceInfo();
			r.decode(bb);
			remove.add(r);
		}
		for (var i = bb.ReadUInt(); i > 0; --i) {
			var r = new BServiceInfo();
			r.decode(bb);
			put.add(r);
		}
		for (var i = bb.ReadUInt(); i > 0; --i) {
			var r = new BServiceInfo();
			r.decode(bb);
			update.add(r);
		}
	}

	private static int _PRE_ALLOC_SIZE_ = 64;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}
}
