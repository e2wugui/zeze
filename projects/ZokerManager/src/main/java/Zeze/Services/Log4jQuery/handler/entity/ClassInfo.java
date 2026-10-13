package Zeze.Services.Log4jQuery.handler.entity;

import java.util.List;

/**
 * 命令参数的类描述：是否基础类型、类名与字段列表（JSON 序列化用）。
 */
public class ClassInfo {
	private boolean baseType;
	private String className;
	private List<SimpleField> fields;

	public boolean isBaseType() {
		return baseType;
	}

	public void setBaseType(boolean baseType) {
		this.baseType = baseType;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public List<SimpleField> getFields() {
		return fields;
	}

	public void setFields(List<SimpleField> fields) {
		this.fields = fields;
	}
}
