package Zeze.log.handle.entity;

/**
 * HTTP API 的通用响应封装：status（状态码）+ desc（说明）+ data（结果数据）。
 */
public class BaseResponse<T> {
	public static final int SUCC = 200;
	public static final int REDIRECT_INDEX = 400;
	public static final int ERROR = 500;

	private static final String SUCC_STR = "success";
	protected int status;
	protected String desc;
	protected T data;

	public static BaseResponse<Object> succResult(Object data) {
		BaseResponse<Object> response = new BaseResponse<>();
		response.status = SUCC;
		response.desc = SUCC_STR;
		response.data = data;
		return response;
	}

	public static BaseResponse<Object> errorResult(String desc) {
		BaseResponse<Object> response = new BaseResponse<>();
		response.status = ERROR;
		response.desc = desc;
		return response;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}
}
