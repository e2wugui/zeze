package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * EditService协议bean：remove（注销）与add（注册/更新）两个服务信息列表，以name+id为key。
 */
public class BEditService implements Serializable {
	private static final List<BServiceInfo> empty = List.of();

	private List<BServiceInfo> remove = empty; // 注销，删除，忽略不存在的。只以name和id为准
	private List<BServiceInfo> add = empty; // 注册，增加或更新。更新时以name和id为key

	// 处理顺序：remove,add。
	// 当不同的集合中存在相同的服务时，要注意这个处理顺序。
	// 也就是说 add 等级更高，服务优先能找到。

	public List<BServiceInfo> getRemove() {
		return remove == empty ? (remove = new ArrayList<>()) : remove;
	}

	public List<BServiceInfo> getAdd() {
		return add == empty ? (add = new ArrayList<>()) : add;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteUInt(remove.size());
		for (var r : remove)
			r.encode(bb);
		bb.WriteUInt(add.size());
		for (var p : add)
			p.encode(bb);
	}

	@Override
	public void decode(IByteBuffer bb) {
		if (remove != empty)
			remove.clear();
		if (add != empty)
			add.clear();
		var i = bb.ReadUInt();
		if (i > 0) {
			var r = getRemove();
			do
				r.add(new BServiceInfo(bb));
			while (--i > 0);
		}
		i = bb.ReadUInt();
		if (i > 0) {
			var p = getAdd();
			do
				p.add(new BServiceInfo(bb));
			while (--i > 0);
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

	@Override
	public String toString() {
		return "BEditService{remove:" + remove + ",add:" + add + "}\n";
	}
}
