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
 * 管理 Bean 的 Map 容器（值类型可变 bean）：结构操作记 LogMap2 增量日志，值 bean 的字段修改记 changed。
 */
public class CollMap2<K, V extends Bean> extends CollMap<K, V> {
	protected final SerializeHelper.CodecFuncs<K> keyCodecFuncs;
	private final BeanValueFactory<V> valueFactory;
	private final int logTypeId;

	public CollMap2(Class<K> keyClass, Class<V> valueClass) {
		keyCodecFuncs = SerializeHelper.createCodec(keyClass);
		valueFactory = BeanValueFactory.of(valueClass);
		logTypeId = LogMap2.typeId(keyClass, valueClass);
	}

	public static <K> CollMap2<K, DynamicBean> dynamic(Class<K> keyClass, DynamicField field) {
		return new CollMap2<>(LogMap2.typeId(keyClass, DynamicBean.class),
				SerializeHelper.createCodec(keyClass), field.values());
	}

	private CollMap2(int logTypeId, SerializeHelper.CodecFuncs<K> keyCodecFuncs, BeanValueFactory<V> valueFactory) {
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
			var mapLog = (LogMap2<K, V>)Transaction.getCurrent().logGetOrAdd(
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
			var mapLog = (LogMap2<K, V>)Transaction.getCurrent().logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			mapLog.remove(key);
		} else
			map = map.minus(key);
	}

	@Override
	public void clear() {
		if (isManaged()) {
			//noinspection unchecked,DataFlowIssue
			var mapLog = (LogMap2<K, V>)Transaction.getCurrent().logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			mapLog.clear();
		} else
			map = Zeze.Collections.Persistents.map();
	}

	@Override
	public void followerApply(Log _log) {
		valueFactory.checkLog(_log);
		@SuppressWarnings("unchecked")
		var log = (LogMap2<K, V>)_log;
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
		map = Objects.requireNonNull(((LogMap2<K, V>)_log).getValue());
	}

	@Override
	public LogBean createLogBean() {
		var log = new LogMap2<>(logTypeId, keyCodecFuncs, valueFactory);
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
	public CollMap2<K, V> copy() {
		var copy = new CollMap2<>(logTypeId, keyCodecFuncs, valueFactory);
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
