package Zeze.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;
import Zeze.Application;
import Zeze.Serialize.SQLStatement;
import Zeze.Util.Factory;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 具体做法是正常定义母表，然后动态表使用相同的 K,V 创建。
 */
public class TableDynamic<K extends Comparable<K>, V extends Bean> extends TableX<K, V> {
	private final @NotNull Function<K, ByteBuffer> keyEncoder;
	private final @NotNull Function<ByteBuffer, K> keyDecoder;
	private final @NotNull Factory<V> valueFactory;
	private final boolean isAutoKey;
	private final @NotNull Class<? extends Comparable<?>> keyClass;
	private final @NotNull Class<? extends Bean> valueClass;

	public TableDynamic(@NotNull Application zeze, @NotNull String tableName, @NotNull TableX<K, V> template) {
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
	public TableDynamic(@NotNull Application zeze, @NotNull String tableName,
						@NotNull Function<K, ByteBuffer> keyEncoder,
						@NotNull Function<ByteBuffer, K> keyDecoder,
						@NotNull Factory<V> valueFactory, boolean isAutoKey, @Nullable String tplTableName,
						@NotNull Class<? extends Comparable<?>> keyClass,
						@NotNull Class<? extends Bean> valueClass) {
		super(Bean.hash32(tableName), tableName);

		this.keyEncoder = keyEncoder;
		this.keyDecoder = keyDecoder;
		this.valueFactory = valueFactory;
		this.isAutoKey = isAutoKey;
		this.keyClass = keyClass;
		this.valueClass = valueClass;

		if (tplTableName == null)
			tplTableName = tableName;
		zeze.openDynamicTable(zeze.getConfig().getTableConf(tplTableName).getDatabaseName(), this);
	}

	@Override
	public @NotNull Class<? extends Comparable<?>> getKeyClass() {
		return keyClass;
	}

	@Override
	public @NotNull Class<? extends Bean> getValueClass() {
		return valueClass;
	}

	@Override
	public @NotNull ByteBuffer encodeKey(@NotNull K key) {
		return keyEncoder.apply(key);
	}

	@Override
	public @NotNull K decodeKey(@NotNull ByteBuffer bb) {
		return keyDecoder.apply(bb);
	}

	@Override
	public @NotNull K decodeKeyResultSet(@NotNull ResultSet rs) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void encodeKeySQLStatement(@NotNull SQLStatement st, @NotNull K _v_) {
		throw new UnsupportedOperationException();
	}

	@Override
	public @NotNull V newValue() {
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
