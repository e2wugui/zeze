package Zeze.Component;

import java.lang.reflect.ParameterizedType;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Net.Binary;
import Zeze.Netty.HttpExchange;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Table;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Transaction.TableX;
import Zeze.Util.JsonReader;
import Zeze.Util.JsonWriter;
import Zeze.Util.Str;
import io.netty.handler.codec.http.HttpResponseStatus;

//TODO: 修改value, 新建记录, 删除记录, 清空表
public class DbWeb extends AbstractDbWeb {
	private Application zeze;
	private String cachedListHtml;

	@Override
	public void Initialize(AppBase app) {
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
					+ "<title>ListTable</title></head><body>\n"
					+ "<script>function f(){var d=document,e=d.getElementById('t');e.value=d.getElementsByName('t')"
					+ "[0].value;}</script><b>DbWeb</b><form action=WalkTable method=get><p>table: <select name=t >\n");
			var tables = new ArrayList<>(zeze.getTables().values());
			tables.sort(Comparator.comparing(Table::getName));
			for (var table : tables) {
				var tableName = table.getName();
				sb.append("<option value=\"").append(tableName).append("\">").append(tableName).append(" (");
				sb.append(((Class<?>)(((ParameterizedType)table.getClass().getGenericSuperclass())
						.getActualTypeArguments()[0])).getSimpleName());
				sb.append(")</option>\n");
			}
			sb.append("</select> <input type=submit value=walk onclick=f() /></form><form action=GetValue method=get>\n"
					+ "key: <input type=hidden id=t name=t /><input name=k /> \n"
					+ "<input type=submit value=get onclick=f() /></form><hr></body></html>\n");
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
			k = JsonReader.local().buf(key).parse((Class<?>)keyType);
		else
			throw new IllegalStateException("ERROR: unsupported key of " + keyType + " for table " + table.getName());
		return k;
	}

	@SuppressWarnings("unchecked")
	public static <K extends Comparable<K>, V extends Bean> K walk(TableX<?, ?> table, Object exclusiveStartKey,
																   int proposeLimit, TableWalkHandle<?, ?> callback) {
		return ((TableX<K, V>)table).walk((K)exclusiveStartKey, proposeLimit, (TableWalkHandle<K, V>)callback);
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
			var keys = new ArrayList<>();
			var lastKey = walk(table, key != null && !key.isEmpty() ? parseKey(table, key) : null, count, (k, __) -> {
				keys.add(k);
				return keys.size() < count;
			});
			var sb = new StringBuilder("<html><head><meta http-equiv=content-type content=text/html;charset=utf-8 />\n"
					+ "<title>WalkTable</title></head><body><style>a{text-decoration:none}</style>\n");
			for (var k : keys) {
				sb.append("<p><a href=\"GetTable?t=").append(tableName).append("&k=")
						.append(URLEncoder.encode(k.toString(), StandardCharsets.UTF_8)).append("\">")
						.append(tableName).append(": ").append(k).append("</a>\n");
			}
			if (lastKey != null) {
				sb.append("<hr><a href=\"WalkTable?t=").append(tableName).append("&k=")
						.append(URLEncoder.encode(lastKey.toString(), StandardCharsets.UTF_8));
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
			var k = parseKey(table, key);
			var jw = JsonWriter.local().clear();
			var oldFlags = jw.getFlags();
			var v = selectDirty(table, k);
			var vs = jw.setPrettyFormat(true).setWrapElement(true).write(v).toString();
			jw.setFlags(oldFlags);
			x.sendPlainText(HttpResponseStatus.OK, vs);
		} catch (Exception e) {
			x.sendPlainText(HttpResponseStatus.OK, Str.stacktrace(e));
		}
	}
}
