package Zeze.Services.Log4jQuery.handler;

public class QueryRequest<T> {
	private String cmd;
	private T param;

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public T getParam() {
		return param;
	}

	public void setParam(T param) {
		this.param = param;
	}
}
