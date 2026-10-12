package Zeze.log.handle.entity;

/**
 * /api/query 的请求参数：目标日志服务器名与透传的查询 json。
 */
public class QueryParam {
	private String serverName;
	private String json;

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getJson() {
		return json;
	}

	public void setJson(String json) {
		this.json = json;
	}
}
