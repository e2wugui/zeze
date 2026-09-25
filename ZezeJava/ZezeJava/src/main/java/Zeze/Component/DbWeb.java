package Zeze.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
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
import Zeze.Util.TaskSpec;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class DbWeb extends AbstractDbWeb {
	private static final Logger logger = LogManager.getLogger(DbWeb.class);

	// token鉴权（FND8-66范式，FND14 comp-02用户裁决）：DbWeb暴露全库读/写/删/清表，挂载在
	// 共享HttpServer上无法按端点绑地址（官方样例不传host即绑0.0.0.0），"内部网络纪律"不可
	// 履行——强制token防误触（端口扫描/错端口curl）优先于防攻击。无参构造保持零配置可用：
	// 自动生成随机token并打日志；Index页保持开放（静态外壳无数据），六个数据端点fail-closed。
	private static final String TOKEN_HEADER = "X-Zeze-Token";
	private static final String TOKEN_QUERY_KEY = "token";
	private final String token;

	public DbWeb() {
		this(null);
	}

	/**
	 * 构造DbWeb。
	 *
	 * @param token 鉴权token；null/空白时自动生成随机token并打日志（默认强制鉴权、零配置可用）
	 */
	public DbWeb(String token) {
		this.token = token != null && !token.isBlank() ? token : generateToken();
		logger.warn("DbWeb enabled: db web endpoints require token"
						+ " (pass via '{}' header or '{}' query param). token={}",
				TOKEN_HEADER, TOKEN_QUERY_KEY, this.token);
	}

	static String generateToken() {
		var bytes = new byte[24];
		new SecureRandom().nextBytes(bytes);
		return Base64.getEncoder().encodeToString(bytes);
	}

	/** token校验：X-Zeze-Token头或token查询参数携带，MessageDigest常量时间比较。 */
	static boolean checkToken(String token, HttpExchange x) {
		var request = x.request();
		var presented = request != null ? request.headers().get(TOKEN_HEADER) : null;
		if (presented == null)
			presented = x.queryMap().get(TOKEN_QUERY_KEY);
		return presented != null && MessageDigest.isEqual(
				token.getBytes(StandardCharsets.UTF_8), presented.getBytes(StandardCharsets.UTF_8));
	}

	/** token失配403并代回应答；返回false时servlet不得继续处理。 */
	private boolean checkAuth(HttpExchange x) {
		if (checkToken(token, x))
			return true;
		logger.warn("DbWeb: reject unauthorized request from {}", x.channel().remoteAddress());
		x.close(x.sendPlainText(HttpResponseStatus.FORBIDDEN, "forbidden"));
		return false;
	}

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
	public void Initialize(@NotNull AppBase app) throws Exception {
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
		if (!checkAuth(x))
			return;
		try {
			var tables = zeze.getTables().values().stream().map(Table::getName).toArray();
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
		if (!checkAuth(x))
			return;
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
		if (!checkAuth(x))
			return;
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
		if (!checkAuth(x))
			return;
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
		if (!checkAuth(x))
			return;
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
		K lastKey = null;
		do {
			// keys必须每轮重建：否则callback从第二轮起恒返回false，每轮只交付1个key且事务重删全部累积（O(n²)）。
			var keys = new ArrayList<K>();
			lastKey = table.walkKey(lastKey, DELETE_BATCH_COUNT, k -> {
				keys.add(k);
				return keys.size() < DELETE_BATCH_COUNT;
			});
			var rc = TaskSpec.ofProcedure(zeze.newProcedure(() -> {
				for (var key : keys)
					table.remove(key);
				return Procedure.Success;
			}, "DbWeb.clearTable")).call();
			// 删除批失败必须中止报错（CP1-F3）：lastKey游标已推进（排他），失败批被静默跳过
			// 后clearTable仍输出"ClearTable done!"，数据静默残留。抛出后OnServletClearTable的
			// catch把异常栈流式发给运维，可见后重跑即可（clearTable幂等）；不做批内重试。
			if (rc != Procedure.Success)
				throw new RuntimeException("clearTable batch fail. table=" + table.getName()
						+ ", batchSize=" + keys.size() + ", rc=" + rc);
			if (batchCallback != null && !batchCallback.test(lastKey))
				break;
		} while (lastKey != null);
	}

	@Override
	protected void OnServletClearTable(HttpExchange x) {
		var beginStream = false;
		try {
			if (!checkAuth(x)) // 守卫必须在beginStream之前：流式应答开始后无法再回403
				return;
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
