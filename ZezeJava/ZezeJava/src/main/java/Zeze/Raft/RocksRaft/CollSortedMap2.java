package Zeze.Raft.RocksRaft;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.HasManagedException;
import Zeze.Serialize.SerializeHelper;

/**
 * 与 {@link CollMap2} 完全对应，但底层使用 {@link org.pcollections.PSortedMap}，
 * 因此 key 必须实现 {@link Comparable}，value 必须是 {@link Bean}。
 * 事务模型与 CollMap2 一致，包括对 Bean 内部修改的 change 跟踪。
 */
public class CollSortedMap2<K extends Comparable<K>, V extends Bean> extends CollSortedMap<K, V> {
	protected final SerializeHelper.CodecFuncs<K> keyCodecFuncs;
	private final BeanValueFactory<V> valueFactory;
	private final int logTypeId;

	public CollSortedMap2(Class<K> keyClass, Class<V> valueClass) {
		keyCodecFuncs = SerializeHelper.createCodec(keyClass);
		valueFactory = BeanValueFactory.of(valueClass);
		logTypeId = LogSortedMap2.typeId(keyClass, valueClass);
	}

	public static <K extends Comparable<K>> CollSortedMap2<K, DynamicBean> dynamic(Class<K> keyClass, DynamicField field) {
		return new CollSortedMap2<>(LogSortedMap2.typeId(keyClass, DynamicBean.class),
				SerializeHelper.createCodec(keyClass), field.values());
	}

	private CollSortedMap2(int logTypeId, SerializeHelper.CodecFuncs<K> keyCodecFuncs, BeanValueFactory<V> valueFactory) {
		this.keyCodecFuncs = keyCodecFuncs;
		this.valueFactory = valueFactory;
		this.logTypeId = logTypeId;
	}

	@Override
	public void add(K key, V value) {
		put(key, value);
	}

	@Override
	public void put(K key, V value) {
		valueFactory.checkValue(value);
		if (isManaged()) {
			// 先挂接再设 mapKey，避免 initRootInfo 失败时污染 bean 的 mapKey。
			value.initRootInfo(rootInfo(), this);
			value.mapKey(key);
			//noinspection unchecked,DataFlowIssue
			var mapLog = (LogSortedMap2<K, V>)Objects.requireNonNull(Transaction.getCurrent()).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			mapLog.put(key, value);
		} else {
			if (value.isManaged())
				throw new HasManagedException();
			value.mapKey(key);
			map = map.plus(key, value);
		}
	}

	@Override
	public void remove(K key) {
		if (isManaged()) {
			//noinspection unchecked,DataFlowIssue
			var mapLog = (LogSortedMap2<K, V>)Transaction.getCurrent().logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			mapLog.remove(key);
		} else
			map = map.minus(key);
	}

	@Override
	public void clear() {
		if (isManaged()) {
			//noinspection unchecked,DataFlowIssue
			var mapLog = (LogSortedMap2<K, V>)Transaction.getCurrent().logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			mapLog.clear();
		} else
			map = map.minusAll(map.keySet()); // 清空仍保留原比较器。
	}

	@Override
	public void followerApply(Log _log) {
		valueFactory.checkLog(_log);
		@SuppressWarnings("unchecked")
		var log = (LogSortedMap2<K, V>)_log;
		var tmp = map;
		var modified = log.getModified();
		// 安装 put 项前补全 mapKey，确保本节点切主后编辑该值时能正确编码 changed 条目。
		HashMap<K, V> puts = null;
		ArrayList<K> removes = null;
		for (var e : modified.entrySet()) {
			var v = e.getValue();
			if (v != null) {
				v.mapKey(e.getKey());
				v.initRootInfo(rootInfo(), this);
				if (puts == null)
					puts = new HashMap<>(modified.size() * 2);
				puts.put(e.getKey(), v);
			} else {
				if (removes == null)
					removes = new ArrayList<>(modified.size());
				removes.add(e.getKey());
			}
		}
		if (puts != null)
			tmp = tmp.plusAll(puts);
		if (removes != null)
			tmp = tmp.minusAll(removes);

		for (var e : log.getChangedWithKey().entrySet()) {
			// encode 侧已过滤无效 key，此处缺失即状态机分歧，直接抛 NPE 由 Rocks.followerApply 统一 fatalKill。
			tmp.get(e.getKey()).followerApply(e.getValue());
		}
		map = tmp;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void leaderApplyNoRecursive(Log _log) {
		map = Objects.requireNonNull(((LogSortedMap2<K, V>)_log).getValue());
	}

	@Override
	public LogBean createLogBean() {
		var log = new LogSortedMap2<>(logTypeId, keyCodecFuncs, valueFactory);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		log.setValue(map);
		return log;
	}

	@Override
	protected void initChildrenRootInfo(Record.@Nullable RootInfo root) {
		for (var v : map.values())
			v.initRootInfo(root, this);
	}

	@Override
	public CollSortedMap2<K, V> copy() {
		var copy = new CollSortedMap2<>(logTypeId, keyCodecFuncs, valueFactory);
		copy.map = getMap();
		return copy;
	}

	@Override
	public void encode(ByteBuffer bb) {
		var tmp = getMap();
		bb.WriteUInt(tmp.size());
		var encoder = keyCodecFuncs.encoder;
		for (var e : tmp.entrySet()) {
			encoder.accept(bb, e.getKey());
			e.getValue().encode(bb);
		}
	}

	@Override
	public void decode(IByteBuffer bb) {
		clear();
		var decoder = keyCodecFuncs.decoder;
		for (int i = bb.ReadUInt(); i > 0; i--)
			put(decoder.apply(bb), valueFactory.read(bb));
	}
}
