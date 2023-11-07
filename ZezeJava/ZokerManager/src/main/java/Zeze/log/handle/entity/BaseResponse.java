package Zeze.log.handle.entity;

public class BaseResponse<T> {
	public static int SUCC = 200;
	public static int REDIRECT_INDEX = 400;
	public static int ERROR = 500;

	private static final String SUCC_STR = "success";
	//状态码
	protected int status;
	//状态 说明
	protected String desc;
	//结果数据
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
