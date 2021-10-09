package Zeze.Gen.Types;

import Zeze.*;
import Zeze.Gen.*;
import java.util.*;

public class Variable {
	private Type Bean;
	public final Type getBean() {
		return Bean;
	}
	private void setBean(Type value) {
		Bean = value;
	}
	private String Name;
	public final String getName() {
		return Name;
	}
	private void setName(String value) {
		Name = value;
	}
	public final String getNamePinyin() {
		return Program.ToPinyin(getName());
	}
	public final String getNamePrivate() {
		return "_" + getName();
	}
	public final String getNameUpper1() {
		return getName().substring(0, 1).toUpperCase() + getName().substring(1);
	}
	private int Id;
	public final int getId() {
		return Id;
	}
	private void setId(int value) {
		Id = value;
	}
	private String Type;
	public final String getType() {
		return Type;
	}
	private void setType(String value) {
		Type = value;
	}
	private String Key;
	public final String getKey() {
		return Key;
	}
	private void setKey(String value) {
		Key = value;
	}
	private String Value;
	public final String getValue() {
		return Value;
	}
	private void setValue(String value) {
		Value = value;
	}
	private String Initial;
	public final String getInitial() {
		return Initial;
	}
	private void setInitial(String value) {
		Initial = value;
	}
	private String Comment;
	public final String getComment() {
		return Comment;
	}
	private void setComment(String value) {
		Comment = value;
	}
	private String Validator;
	public final String getValidator() {
		return Validator;
	}
	private void setValidator(String value) {
		Validator = value;
	}
	private boolean AllowNegative = false;
	public final boolean getAllowNegative() {
		return AllowNegative;
	}
	private void setAllowNegative(boolean value) {
		AllowNegative = value;
	}

	public final String GetBeanFullName() {
		if (getBean() instanceof Bean) {
			return ((Bean)getBean()).getFullName();
		}

		if (getBean() instanceof BeanKey) {
			return ((BeanKey)getBean()).getFullName();
		}

		throw new RuntimeException("Variable holder is not a bean");
	}

	public Variable(Type bean, XmlElement self) {
		setBean(bean);
		setName(self.GetAttribute("name").strip());
		Program.CheckReserveName(getName());
		setId(Integer.parseInt(self.GetAttribute("id")));
		if (getId() <= 0 || getId() > Zeze.Transaction.Bean.MaxVariableId) {
			throw new RuntimeException("variable id invalid. range [1, " + Zeze.Transaction.Bean.MaxVariableId + "] @" + GetBeanFullName());
		}
		setType(self.GetAttribute("type").strip());
		if (tangible.StringHelper.isNullOrEmpty(getType())) {
			throw new RuntimeException("Variable Type Can Not Be Empty.");
		}
		setKey(self.GetAttribute("key").strip());
		setValue(self.GetAttribute("value").strip());
		setInitial(self.GetAttribute("default").strip());
		setValidator(self.GetAttribute("validator").strip());
		String attr = self.GetAttribute("AllowNegative");
		if (attr.length() > 0) {
			setAllowNegative(Boolean.parseBoolean(attr));
		}

		setComment(self.GetAttribute("comment"));
		if (getComment().length() == 0) {
			XmlNode c = self.NextSibling;
			if (c != null && XmlNodeType.Text == c.NodeType) {
				setComment(c.InnerText.strip());
				Regex regex = new Regex("[\r\n]");
				setComment(regex.Replace(getComment(), ""));
			}
		}
		if (getComment().length() > 0) {
			setComment(" // " + getComment());
		}

		HashSet<String> dynamicValue = new HashSet<String>();
		XmlNodeList childNodes = self.ChildNodes;
		for (XmlNode node : childNodes) {
			if (XmlNodeType.Element != node.NodeType) {
				continue;
			}

			XmlElement e = (XmlElement)node;

			String nodename = e.Name;
			switch (e.Name) {
				case "value":
					dynamicValue.add(e.GetAttribute("bean"));
					break;
				default:
					throw new RuntimeException("node=" + nodename);
			}
		}
		for (String b : getValue().split("[,]", -1)) {
			dynamicValue.add(b.strip());
		}
		StringBuilder valueBuilder = new StringBuilder();
		boolean first = true;
		for (String b : dynamicValue) {
			if (b.length() == 0) {
				continue;
			}
			if (first) {
				first = false;
			}
			else {
				valueBuilder.append(',');
			}
			valueBuilder.append(b);
		}
		setValue(valueBuilder.toString());
	}

	private Type VariableType;
	public final Type getVariableType() {
		return VariableType;
	}
	private void setVariableType(Type value) {
		VariableType = value;
	}

	public final void Compile(ModuleSpace space) {
		setVariableType(Types.Type.Compile(space, getType(), getKey(), getValue()));
	}
}