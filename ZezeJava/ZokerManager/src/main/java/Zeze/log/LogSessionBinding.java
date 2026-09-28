package Zeze.log;

/**
 * Search/Browse 会话的数据源绑定回执（会话回执比对）。
 * 会话创建时记录 (会话类型, serverName, logName) 三元组，HTTP 层复用会话前用
 * {@link #matches} 比对请求参数，不匹配即视为新会话（关旧建新）——服务端自证，
 * 不依赖前端"切换数据源必须置 changeSession"的客户端侧契约。
 * Session/SessionAll 在 Zeze.Services.Log4jQuery（协议侧），绑定回执属于
 * HTTP 层的会话身份管理，故记录在 ZokerManager 侧随会话对象一并存表。
 */
public record LogSessionBinding(boolean all, String serverName, String logName, Object session) {

	/** 指定 server 的单服务器会话绑定（Session）。logName 归一 null→""。 */
	public static LogSessionBinding server(String serverName, String logName, Object session) {
		return new LogSessionBinding(false, serverName, normalize(logName), session);
	}

	/** 全服视图会话绑定（SessionAll，serverName 无意义，归一为空串）。 */
	public static LogSessionBinding allView(String logName, Object session) {
		return new LogSessionBinding(true, "", normalize(logName), session);
	}

	/** 请求三元组与绑定记录是否一致（serverName 按原样比较；logName 归一 null→"" 后比较）。 */
	public boolean matches(boolean requestAll, String requestServerName, String requestLogName) {
		if (all != requestAll)
			return false;
		if (all)
			return logName.equals(normalize(requestLogName));
		return serverName.equals(requestServerName) && logName.equals(normalize(requestLogName));
	}

	private static String normalize(String s) {
		return s == null ? "" : s;
	}
}
