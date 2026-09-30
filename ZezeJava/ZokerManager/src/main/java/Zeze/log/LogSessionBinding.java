package Zeze.log;

/**
 * Search/Browse 会话的数据源绑定回执（会话回执比对）。
 * 会话创建时记录 (会话类型, serverName, logName, 查询条件指纹) 四元组，HTTP 层
 * 复用会话前用 {@link #matches} 比对请求参数，不匹配即视为新会话（关旧建新）——
 * 服务端自证，不依赖前端"切换数据源/改条件必须置 changeSession"的客户端侧契约。
 * Session/SessionAll 在 Zeze.Services.Log4jQuery（协议侧），绑定回执属于
 * HTTP 层的会话身份管理，故记录在 ZokerManager 侧随会话对象一并存表。
 * 条件指纹：数据源三元组之外，查询条件（words/pattern/containsType/beginTime/
 * endTime，browse 另含 offsetFactor）也参与会话身份——服务端仅 beginTime 有去重
 * 哨兵，其余条件变更复用旧游标会话会静默漏掉游标之前的匹配。指纹由
 * {@code SearchLogParam.conditionFingerprint} 归一生成（值语义：同条件翻页指纹
 * 稳定，条件变即视同 changeSession 关旧建新，重建丢游标/水位正是"新条件新查询"
 * 从头求值的应有语义）。
 * 另记最后活跃时间（创建与复用命中时刷新，System.nanoTime 单调时基）：
 * FileSessionManager 的闲置清扫据此判闲置驱逐。
 * 全服视图（all）另记创建时刻的成员集键串快照（zoker-04 起为注册表键集，FND30
 * zokermanager-02 起为会话实际成员集——注册表键集不可证明会话构成：构造期跳过/
 * 注册竞态窗口）：摘除下台后复用旧会话=成员集漂移，resolve 复用前以会话实际成员集
 * 比对当前注册表收敛（多余成员走重建缩容，缺失成员由 SessionAll.operate 的缺册补员
 * 自愈，见 FileSessionManager.allViewMembersConverged）。快照仅供会话非 SessionAll 的
 * 直构形态回退比对；空串快照（未记录，如直构测试形态）不可证明成员不变，比对恒不
 * 匹配（安全方向：重建）。
 */
public record LogSessionBinding(boolean all, String serverName, String logName, String conditionKey,
								Object session, long lastActiveNanos, String allServersKey) {

	/** 指定 server 的单服务器会话绑定（Session）。logName 归一 null→""；lastActiveNanos=创建时刻；
	 * allServersKey 对单服务器视图无集合语义，恒空串。 */
	public static LogSessionBinding server(String serverName, String logName, String conditionKey, Object session) {
		return new LogSessionBinding(false, serverName, normalize(logName), conditionKey, session,
				System.nanoTime(), "");
	}

	/** 全服视图会话绑定（SessionAll，serverName 无意义，归一为空串）；lastActiveNanos=创建时刻。
	 * 不携带键集快照（空串=未记录：resolve 侧不可证明成员不变，视同漂移走重建）——
	 * 供无 LogAgent 的直构形态；HTTP 路径用 {@link #allView(String, String, Object, String)}。 */
	public static LogSessionBinding allView(String logName, String conditionKey, Object session) {
		return new LogSessionBinding(true, "", normalize(logName), conditionKey, session, System.nanoTime(), "");
	}

	/** 全服视图会话绑定，携带创建时刻的服务器键集快照（resolve 复用前比对当前键集，见类注释）。 */
	public static LogSessionBinding allView(String logName, String conditionKey, Object session, String allServersKey) {
		return new LogSessionBinding(true, "", normalize(logName), conditionKey, session, System.nanoTime(),
				allServersKey);
	}

	/** 复用命中时刷新活跃时间的副本：四元组、会话与键集快照不变，仅时间戳前移（调用方条件 replace 回写）。 */
	public LogSessionBinding touched(long nowNanos) {
		return new LogSessionBinding(all, serverName, logName, conditionKey, session, nowNanos, allServersKey);
	}

	/** 请求四元组与绑定记录是否一致（serverName 按原样比较；logName 归一 null→"" 后比较；
	 * 条件指纹按值比较——null 请求指纹仅直构测试形态，恒不匹配走重建，安全方向）。
	 * 键集快照不在此比对（matches 无 LogAgent 可取当前键集），由 FileSessionManager.resolve 比对。 */
	public boolean matches(boolean requestAll, String requestServerName, String requestLogName,
						   String requestConditionKey) {
		if (all != requestAll)
			return false;
		if (!conditionKey.equals(requestConditionKey))
			return false;
		if (all)
			return logName.equals(normalize(requestLogName));
		return serverName.equals(requestServerName) && logName.equals(normalize(requestLogName));
	}

	private static String normalize(String s) {
		return s == null ? "" : s;
	}
}
