package Zeze.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Net.Binary;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Table;
import Zeze.Transaction.TableDynamic;
import Zeze.Transaction.TableWalkKey;
import Zeze.Transaction.TableX;
import Zeze.Util.Json;
import Zeze.Util.JsonWriter;
import Zeze.Util.OutLong;
import Zeze.Util.Str;
import Zeze.Util.Task;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DbWeb extends AbstractDbWeb {
	private static final Logger logger = LogManager.getLogger(DbWeb.class);

	private Application zeze;
	private String indexHtml;

	public static String toJsonForView(Object obj) {
		var jw = JsonWriter.local();
		try {
			return jw.clear().setFlagsAndDepthLimit(JsonWriter.FLAG_PRETTY_FORMAT_AND_WRAP_ELEMENT, 16)
					.write(obj).toString();
		} finally {
			jw.clear();
		}
	}

	public static String toJsonForCompact(Object obj) {
		var jw = JsonWriter.local();
		try {
			return jw.clear().setFlagsAndDepthLimit(JsonWriter.FLAG_NO_QUOTE_KEY, 16).write(obj).toString();
		} finally {
			jw.clear();
		}
	}

	@Override
	public void Initialize(AppBase app) throws Exception {
		logger.debug("start db web");
		super.Initialize(app);
		zeze = app.getZeze();

		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("DbWebIndex.html")) {
			if (inputStream != null) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
					indexHtml = reader.lines().collect(Collectors.joining("\n"));
				}
			} else {
				logger.info("DbWebIndex.html not found");
			}
		}
	}

	public void SetIndexHtml(String indexHtml) {
		Objects.requireNonNull(indexHtml);
		this.indexHtml = indexHtml;
	}

	@Override
	protected void OnServletIndex(HttpExchange x) {
		x.sendHtml(HttpResponseStatus.OK, indexHtml);
	}

	@Override
	protected void OnServletListTable(HttpExchange x) {
		try {
			List<String> tables = zeze.getTables().values().stream().map(Table::getName).toList();
			x.sendJson(HttpResponseStatus.OK, toJsonForView(tables));
		} catch (Exception e) {
			x.sendPlainText(HttpResponseStatus.OK, Str.stacktrace(e));
		}
	}

	private static Object parseKey(TableX<?, ?> table, String key) {
		Object k;
		Type keyType;
		if (table instanceof TableDynamic)
			keyType = table.getKeyClass();
		else
			keyType = ((ParameterizedType)table.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		if (keyType == Long.class)
			k = Long.parseLong(key);
		else if (keyType == Integer.class)
			k = Integer.parseInt(key);
		else if (keyType == String.class)
			k = key;
		else if (keyType == Binary.class)
			k = new Binary(key);
		else if (keyType instanceof Class && Serializable.class.isAssignableFrom((Class<?>)keyType)) // BeanKey
			k = Json.parse(key, (Class<?>)keyType);
		else
			throw new IllegalStateException("ERROR: unsupported key of " + keyType + " for table " + table.getName());
		return k;
	}

	@SuppressWarnings("unchecked")
	public static <K extends Comparable<K>, V extends Bean> K walkKey(TableX<?, ?> table, Object exclusiveStartKey,
																	  int proposeLimit, TableWalkKey<?> callback) throws Exception {
		return ((TableX<K, V>)table).walkKey((K)exclusiveStartKey, proposeLimit, (TableWalkKey<K>)callback);
	}

	static class WalkTableResult {
		public List<String> keys;
		public boolean hasMore;
	}

	@Override
	protected void OnServletWalkTable(HttpExchange x) {
		try {
			var qm = x.queryMap();
			var tableName = qm.get("t");
			var key = qm.get("k");
			var n = qm.get("n");
			Objects.requireNonNull(tableName, "tableName");
			var count = n != null ? Math.min(Integer.parseInt(n), 1_000_000) : 100;
			var table = (TableX<?, ?>)zeze.getTable(tableName);
			if (table == null) {
				x.sendPlainText(HttpResponseStatus.OK, "not found table: \"" + tableName + '"');
				return;
			}
			List<String> keys = new ArrayList<>();
			var lastKey = walkKey(table, key != null && !key.isEmpty() ? parseKey(table, key) : null, count, k -> {
				var ks = k instanceof Serializable ? toJsonForCompact(k) : k.toString();
				keys.add(ks);
				return keys.size() < count;
			});

			boolean hasMore = (lastKey != null && keys.size() >= count);

			WalkTableResult res = new WalkTableResult();
			res.keys = keys;
			res.hasMore = hasMore;

			x.sendJson(HttpResponseStatus.OK, toJsonForView(res));
		} catch (Exception e) {
			x.sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, Str.stacktrace(e));
		}
	}

	@SuppressWarnings("unchecked")
	private static <K extends Comparable<K>> Bean selectDirty(TableX<?, ?> table, Object key) {
		return ((TableX<K, ?>)table).selectDirty((K)key);
	}

	@Override
	protected void OnServletGetValue(HttpExchange x) {
		try {
			var qm = x.queryMap();
			var tableName = qm.get("t");
			var key = qm.get("k");
			Objects.requireNonNull(tableName, "tableName");
			Objects.requireNonNull(key, "key");
			var table = (TableX<?, ?>)zeze.getTable(tableName);
			if (table == null) {
				x.sendPlainText(HttpResponseStatus.OK, "not found table: \"" + tableName + '"');
				return;
			}
			var k = parseKey(table, key);
			var v = selectDirty(table, k);
			x.sendJson(HttpResponseStatus.OK, toJsonForView(v));
		} catch (Exception e) {
			x.sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, Str.stacktrace(e));
		}
	}

	@SuppressWarnings("unchecked")
	private static <K extends Comparable<K>, V extends Bean> void put(TableX<?, ?> table, Object key, Object value) {
		((TableX<K, V>)table).put((K)key, (V)value);
	}

	@Override
	protected void OnServletPutRecord(HttpExchange x) {
		try {
			var qm = x.queryMap();
			var tableName = qm.get("t");
			var key = qm.get("k");
			String value = x.contentString();
			Objects.requireNonNull(tableName, "tableName");
			Objects.requireNonNull(key, "key");
			Objects.requireNonNull(value, "value");
			var table = (TableX<?, ?>)zeze.getTable(tableName);
			if (table == null) {
				x.sendPlainText(HttpResponseStatus.OK, "not found table: \"" + tableName + '"');
				return;
			}
			var k = parseKey(table, key);
			var valueClass = (Class<?>)
					((ParameterizedType)table.getClass().getGenericSuperclass()).getActualTypeArguments()[1];
			var v = Json.parse(value, valueClass);
			if (v == null) {
				x.sendPlainText(HttpResponseStatus.BAD_REQUEST, "parse value failed!");
				return;
			}
			put(table, k, v);
			x.sendPlainText(HttpResponseStatus.OK, "PutRecord done!");
		} catch (Exception e) {
			x.sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, Str.stacktrace(e));
		}
	}

	@SuppressWarnings("unchecked")
	private static <K extends Comparable<K>> void remove(TableX<?, ?> table, Object key) {
		((TableX<K, ?>)table).remove((K)key);
	}

	@Override
	protected void OnServletDeleteRecord(HttpExchange x) {
		try {
			var qm = x.queryMap();
			var tableName = qm.get("t");
			var key = qm.get("k");
			Objects.requireNonNull(tableName, "tableName");
			Objects.requireNonNull(key, "key");
			var table = (TableX<?, ?>)zeze.getTable(tableName);
			if (table == null) {
				x.sendPlainText(HttpResponseStatus.BAD_REQUEST, "not found table: \"" + tableName + '"');
				return;
			}
			var k = parseKey(table, key);
			remove(table, k);
			x.sendPlainText(HttpResponseStatus.OK, "DeleteRecord done!");
		} catch (Exception e) {
			x.sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, Str.stacktrace(e));
		}
	}

	public <K extends Comparable<K>, V extends Bean> void clearTable(TableX<K, V> table, Predicate<K> batchCallback) throws Exception {
		final var DELETE_BATCH_COUNT = 100;
		var keys = new ArrayList<K>();
		K lastKey = null;
		do {
			lastKey = table.walkKey(lastKey, DELETE_BATCH_COUNT, k -> {
				keys.add(k);
				return keys.size() < DELETE_BATCH_COUNT;
			});
			Task.call(zeze.newProcedure(() -> {
				for (var key : keys)
					table.remove(key);
				return Procedure.Success;
			}, "DbWeb.clearTable"));
			if (batchCallback != null && !batchCallback.test(lastKey))
				break;
		} while (lastKey != null);
	}

	@Override
	protected void OnServletClearTable(HttpExchange x) {
		var beginStream = false;
		try {
			var qm = x.queryMap();
			var tableName = qm.get("t");
			Objects.requireNonNull(tableName, "tableName");
			var table = (TableX<?, ?>)zeze.getTable(tableName);
			x.beginStream(HttpResponseStatus.OK, HttpServer.setDate(new DefaultHttpHeaders())
					.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE));
			beginStream = true;
			x.sendStream(("ClearTable '" + tableName + "' begin ...\n").getBytes(StandardCharsets.UTF_8));
			if (table != null) {
				var t = new OutLong(System.nanoTime());
				clearTable(table, lastKey -> {
					var t1 = System.nanoTime();
					if (t1 - t.value >= 1_000_000_000L && lastKey != null) {
						t.value = t1;
						x.sendStream(("  key: " + lastKey).getBytes(StandardCharsets.UTF_8));
					}
					return true;
				});
				x.sendStream("ClearTable done!".getBytes(StandardCharsets.UTF_8));
			} else
				x.sendStream("not found table!".getBytes(StandardCharsets.UTF_8));
			x.endStream();
		} catch (Exception e) {
			if (beginStream) {
				x.sendStream(Str.stacktrace(e).getBytes(StandardCharsets.UTF_8));
				x.endStream();
			} else
				x.sendPlainText(HttpResponseStatus.OK, Str.stacktrace(e));
		}
	}
}
