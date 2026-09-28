package Zeze.Services.Log4jQuery.handler.entity;

/**
 * 字段描述：名称与类型名（JSON 序列化用）。
 */
public class SimpleField {
	private String name;
	private String type;

	public SimpleField(String name, String type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
