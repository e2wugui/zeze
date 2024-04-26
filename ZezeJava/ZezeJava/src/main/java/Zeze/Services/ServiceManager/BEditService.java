package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class BEditService extends Bean {
	private static final List<BServiceInfo> empty = List.of();

	private @NotNull List<BServiceInfo> remove = empty; // 注销，删除，忽略不存在的。
	private @NotNull List<BServiceInfo> put = empty; // 注册，增加或替换。
	private @NotNull List<BServiceInfo> update = empty; // 更新，忽略不存在的，只更新局部信息。

	// 处理顺序：remove,put,update。
	// 当不同的集合中存在相同的服务时，要注意这个处理顺序。
	// 也就是说 put 等级更高，服务优先能找到。
	// update处理是特殊的，放在put后面，使得可以一次注册同时马上更新。

	public @NotNull List<BServiceInfo> getRemove() {
		return remove == empty ? (remove = new ArrayList<>()) : remove;
	}

	public @NotNull List<BServiceInfo> getPut() {
		return put == empty ? (put = new ArrayList<>()) : put;
	}

	public @NotNull List<BServiceInfo> getUpdate() {
		return update == empty ? (update = new ArrayList<>()) : update;
	}

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
		var i = bb.ReadUInt();
		if (i > 0) {
			var r = getRemove();
			do
				r.add(new BServiceInfo(bb));
			while (--i > 0);
		}
		i = bb.ReadUInt();
		if (i > 0) {
			var p = getPut();
			do
				p.add(new BServiceInfo(bb));
			while (--i > 0);
		}
		i = bb.ReadUInt();
		if (i > 0) {
			var u = getUpdate();
			do
				u.add(new BServiceInfo(bb));
			while (--i > 0);
		}
	}

	@Override
	public String toString() {
		return "remove:" + remove + "\n" +
				"put:" + put + "\n" +
				"update:" + update + "\n";
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
