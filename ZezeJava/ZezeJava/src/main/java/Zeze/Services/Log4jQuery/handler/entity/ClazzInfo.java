package Zeze.Services.Log4jQuery.handler.entity;

import java.util.List;

public class ClazzInfo {
	private boolean isBaseType;
	private String clazzName;
	private List<SimpleField> fields;

	public boolean isBaseType() {
		return isBaseType;
	}

	public void setBaseType(boolean baseType) {
		isBaseType = baseType;
	}

	public String getClazzName() {
		return clazzName;
	}

	public void setClazzName(String clazzName) {
		this.clazzName = clazzName;
	}

	public List<SimpleField> getFields() {
		return fields;
	}

	public void setFields(List<SimpleField> fields) {
		this.fields = fields;
	}
}
