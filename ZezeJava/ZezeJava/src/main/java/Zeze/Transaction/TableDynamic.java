package Zeze.Transaction;

import java.util.Objects;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;
import Zeze.Application;
import Zeze.Serialize.SQLStatement;
import Zeze.Util.Factory;
import Zeze.Serialize.ByteBuffer;
import org.jspecify.annotations.Nullable;

/**
 * 具体做法是正常定义母表，然后动态表使用相同的 K,V 创建。
 */
public class TableDynamic<K extends Comparable<K>, V extends Bean> extends TableX<K, V> {
	private final Function<K, ByteBuffer> keyEncoder;
	private final Function<ByteBuffer, K> keyDecoder;
	private final Factory<V> valueFactory;
	private final boolean isAutoKey;
	private final Class<? extends Comparable<?>> keyClass;
	private final Class<? extends Bean> valueClass;

	public TableDynamic(Application zeze, String tableName, TableX<K, V> template) {
		this(zeze, tableName,
				template::encodeKey,
				template::decodeKey,
				template::newValue,
				template.isAutoKey(),
				template.getName(),
				template.getKeyClass(),
				template.getValueClass());
	}

	/**
	 * 创建动态表。
	 * 创建之后就能使用。
	 * 动态表的 schemas 检查由母表完成。
	 *
	 * @param tplTableName 模板表名, 如果为null，则使用tableName作为配置名。
	 */
	public TableDynamic(Application zeze, String tableName,
						Function<K, ByteBuffer> keyEncoder,
						Function<ByteBuffer, K> keyDecoder,
						Factory<V> valueFactory, boolean isAutoKey, @Nullable String tplTableName,
						Class<? extends Comparable<?>> keyClass,
						Class<? extends Bean> valueClass) {
		super(Bean.hash32(tableName), tableName);

		this.keyEncoder = keyEncoder;
		this.keyDecoder = keyDecoder;
		this.valueFactory = valueFactory;
		this.isAutoKey = isAutoKey;
		this.keyClass = keyClass;
		this.valueClass = valueClass;

		if (tplTableName == null)
			tplTableName = tableName;
		zeze.openDynamicTable(Objects.requireNonNull(zeze.getConfig().getTableConf(tplTableName)).getDatabaseName(), this);
	}

	@Override
	public Class<? extends Comparable<?>> getKeyClass() {
		return keyClass;
	}

	@Override
	public Class<? extends Bean> getValueClass() {
		return valueClass;
	}

	@Override
	public ByteBuffer encodeKey(K key) {
		return keyEncoder.apply(key);
	}

	@Override
	public K decodeKey(ByteBuffer bb) {
		return keyDecoder.apply(bb);
	}

	@Override
	public K decodeKeyResultSet(ResultSet rs) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void encodeKeySQLStatement(SQLStatement st, K _v_) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V newValue() {
		return valueFactory.create();
	}

	@Override
	public boolean isAutoKey() {
		return isAutoKey;
	}

	public void dropTable() {
		var storage = getStorage();
		if (storage == null)
			throw new UnsupportedOperationException();
		storage.getDatabaseTable().drop();
	}
}
