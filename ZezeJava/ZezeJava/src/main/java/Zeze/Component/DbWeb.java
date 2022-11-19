package Zeze.Component;

import java.lang.reflect.ParameterizedType;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Net.Binary;
import Zeze.Netty.HttpExchange;
import Zeze.Transaction.Bean;
import Zeze.Transaction.TableX;
import Zeze.Util.JsonReader;
import Zeze.Util.JsonWriter;
import Zeze.Util.Str;
import io.netty.handler.codec.http.HttpResponseStatus;

//TODO: 修改value, 新建记录, 删除记录, 清空表, 从某个key开始walk
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
			var sb = new StringBuilder("<html><head><meta http-equiv=content-type content=text/html;charset=utf-8 />");
			sb.append("\n<title>ListTable</title></head><body>\n");
			sb.append("<script>function f(){var d=document,e=d.getElementById('t');e.value=d.getElementsByName('t')");
			sb.append("[0].value;}</script><b>DbWeb</b><form action=WalkTable method=get><p>table: <select name=t >\n");
			var tableNames = new ArrayList<String>();
			for (var table : zeze.getTables().values())
				tableNames.add(table.getName());
			Collections.sort(tableNames);
			for (var tableName : tableNames)
				sb.append("<option value=\"").append(tableName).append("\">").append(tableName).append("</option>\n");
			sb.append("</select> <input type=submit value=walk onclick=f() /></form><form action=GetValue method=get>");
			sb.append("\nkey: <input type=hidden id=t name=t /><input name=k /> \n");
			sb.append("<input type=submit value=get onclick=f() /></form><hr></body></html>\n");
			cachedListHtml = html = sb.toString();
			x.sendHtml(HttpResponseStatus.OK, html);
		} catch (Exception e) {
			x.sendPlainText(HttpResponseStatus.OK, Str.stacktrace(e));
		}
	}

	@Override
	protected void OnServletWalkTable(HttpExchange x) {
		try {
			var qm = x.queryMap();
			var tableName = qm.get("t");
			var n = qm.get("n");
			Objects.requireNonNull(tableName, "tableName");
			var count = n != null ? Math.min(Integer.parseInt(n), 1_000_000) : 100;
			var table = (TableX<?, ?>)zeze.getTable(tableName);
			var keys = new ArrayList<>();
			table.walk((k, __) -> {
				keys.add(k);
				return keys.size() < count;
			});
			var sb = new StringBuilder("<html><head><meta http-equiv=content-type content=text/html;charset=utf-8 />");
			sb.append("\n<title>walkTable</title></head><body><style>a{text-decoration:none}</style>\n");
			for (var k : keys) {
				sb.append("<p><a href=\"GetTable?t=").append(tableName).append("&k=")
						.append(URLEncoder.encode(k.toString(), StandardCharsets.UTF_8)).append("\">")
						.append(tableName).append(": ").append(k).append("</a>\n");
			}
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
			else if (Bean.class.isAssignableFrom((Class<?>)keyType))
				k = JsonReader.local().buf(key).parse((Class<?>)keyType);
			else {
				x.sendPlainText(HttpResponseStatus.OK,
						"ERROR: unsupported key of " + keyType + " for table " + tableName);
				return;
			}
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
