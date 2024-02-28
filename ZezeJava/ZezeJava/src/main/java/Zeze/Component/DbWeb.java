package Zeze.Component;

import java.lang.reflect.ParameterizedType;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;
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
	private String cachedListHtml;

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
		super.Initialize(app);
		zeze = app.getZeze();
	}

	@Override
	protected void OnServletListTable(HttpExchange x) {
		try {
			var html = cachedListHtml;
			if (html != null) {
				x.sendHtml(HttpResponseStatus.OK, html);
				return;
			}
			var sb = new StringBuilder("<html><head><meta http-equiv=content-type content=text/html;charset=utf-8 />\n"
					+ "<title>ListTable</title></head><body><script>var d=document;\n"
					+ "function c(){d.getElementById('k').name='';d.getElementById('v').name='';\n"
					+ "e=d.getElementById('f');e.method='get';e.action='ClearTable';return confirm('confirm?');}\n"
					+ "function g(){d.getElementById('k').name='k';d.getElementById('v').name='';\n"
					+ "e=d.getElementById('f');e.method='get';e.action='GetValue';}\n"
					+ "function w(){d.getElementById('k').name='k';d.getElementById('v').name='';\n"
					+ "e=d.getElementById('f');e.method='get';e.action='WalkTable';}\n"
					+ "function r(){d.getElementById('k').name='k';d.getElementById('v').name='';\n"
					+ "e=d.getElementById('f');e.method='get';e.action='DeleteRecord';return confirm('confirm?');}\n"
					+ "function p(){d.getElementById('k').name='k';d.getElementById('v').name='v';\n"
					+ "e=d.getElementById('f');e.method='post';e.action='PutRecord';return confirm('confirm?');}\n"
					+ "</script><b>DbWeb</b><form id=f action=a method=get><p>table: <select name=t >\n");
			var tables = new ArrayList<>(zeze.getTables().values());
			tables.sort(Comparator.comparing(Table::getName));
			for (var table : tables) {
				var tableName = table.getName();
				sb.append("<option value=\"").append(tableName).append("\">").append(tableName).append(" (");
				if (table instanceof TableDynamic)
					sb.append("TableDynamic");
				else {
					try {
						sb.append(((Class<?>)(((ParameterizedType)table.getClass().getGenericSuperclass())
								.getActualTypeArguments()[0])).getSimpleName());
					} catch (Exception e) {
						logger.error("unexpected key type: {}", tableName, e);
						sb.append('?');
					}
				}
				sb.append(")</option>\n");
			}
			sb.append("</select> <input type=submit value=clear onclick=\"return c()\"/><p>key: <input id=k />\n"
					+ "<input type=submit value=get onclick=g() /> <input type=submit value=walk onclick=w() />\n"
					+ "<input type=submit value=delete onclick=\"return r()\"/><p>value(json): \n"
					+ "<input type=submit value=put onclick=\"return p()\"/><br>\n"
					+ "<textarea id=v style=\"width:400px;height:400px;\"></textarea></form><hr></body></html>\n");
			cachedListHtml = html = sb.toString();
			x.sendHtml(HttpResponseStatus.OK, html);
		} catch (Exception e) {
			x.sendPlainText(HttpResponseStatus.OK, Str.stacktrace(e));
		}
	}

	private static Object parseKey(TableX<?, ?> table, String key) throws ReflectiveOperationException {
		Object k;
		var keyType = ((ParameterizedType)table.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		if (keyType == Long.class)
			k = Long.parseLong(key);
		else if (keyType == Integer.class)
			k = Integer.parseInt(key);
		else if (keyType == String.class)
			k = key;
		else if (keyType == Binary.class)
			k = new Binary(key);
		else if (Serializable.class.isAssignableFrom((Class<?>)keyType)) // BeanKey
			k = Json.parse(key, (Class<?>)keyType);
		else
			throw new IllegalStateException("ERROR: unsupported key of " + keyType + " for table " + table.getName());
		return k;
	}

	@SuppressWarnings("unchecked")
	public static <K extends Comparable<K>, V extends Bean> K walkKey(TableX<?, ?> table, Object exclusiveStartKey,
																	  int proposeLimit, TableWalkKey<?> callback) {
		return ((TableX<K, V>)table).walkKey((K)exclusiveStartKey, proposeLimit, (TableWalkKey<K>)callback);
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
			var keys = new ArrayList<>();
			var lastKey = walkKey(table, key != null && !key.isEmpty() ? parseKey(table, key) : null, count, k -> {
				keys.add(k);
				return keys.size() < count;
			});
			var sb = new StringBuilder("<html><head><meta http-equiv=content-type content=text/html;charset=utf-8 />\n"
					+ "<title>WalkTable</title></head><body><style>a{text-decoration:none}</style>\n");
			for (var k : keys) {
				var ks = k instanceof Serializable ? toJsonForCompact(k) : k.toString();
				sb.append("<p><a href=\"GetValue?t=").append(tableName).append("&k=")
						.append(URLEncoder.encode(ks, StandardCharsets.UTF_8)).append("\">")
						.append(tableName).append(": ").append(ks).append("</a>\n");
			}
			if (lastKey != null && keys.size() >= count) {
				var ks = lastKey instanceof Serializable ? toJsonForCompact(lastKey) : lastKey.toString();
				sb.append("<hr><a href=\"WalkTable?t=").append(tableName).append("&k=")
						.append(URLEncoder.encode(ks, StandardCharsets.UTF_8));
				if (n != null)
					sb.append("&n=").append(n);
				sb.append("\">NEXT</a>\n");
			} else
				sb.append("<hr>END\n");
			sb.append("<hr></body></html>\n");
			x.sendHtml(HttpResponseStatus.OK, sb.toString());
		} catch (Exception e) {
			x.sendPlainText(HttpResponseStatus.OK, Str.stacktrace(e));
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
			x.sendPlainText(HttpResponseStatus.OK, toJsonForView(v));
		} catch (Exception e) {
			x.sendPlainText(HttpResponseStatus.OK, Str.stacktrace(e));
		}
	}

	@SuppressWarnings("unchecked")
	private static <K extends Comparable<K>, V extends Bean> void put(TableX<?, ?> table, Object key, Object value) {
		((TableX<K, V>)table).put((K)key, (V)value);
	}

	@Override
	protected void OnServletPutRecord(HttpExchange x) {
		try {
			var qm = x.contentQueryMap();
			var tableName = qm.get("t");
			var key = qm.get("k");
			var value = qm.get("v");
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
				x.sendPlainText(HttpResponseStatus.OK, "parse value failed!");
				return;
			}
			put(table, k, v);
			x.sendPlainText(HttpResponseStatus.OK, "PutRecord done!");
		} catch (Exception e) {
			x.sendPlainText(HttpResponseStatus.OK, Str.stacktrace(e));
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
				x.sendPlainText(HttpResponseStatus.OK, "not found table: \"" + tableName + '"');
				return;
			}
			var k = parseKey(table, key);
			remove(table, k);
			x.sendPlainText(HttpResponseStatus.OK, "DeleteRecord done!");
		} catch (Exception e) {
			x.sendPlainText(HttpResponseStatus.OK, Str.stacktrace(e));
		}
	}

	public <K extends Comparable<K>, V extends Bean> void clearTable(TableX<K, V> table, Predicate<K> batchCallback) {
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
			//noinspection VulnerableCodeUsages
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
