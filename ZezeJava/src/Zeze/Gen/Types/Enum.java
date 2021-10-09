package Zeze.Gen.Types;

import Zeze.*;
import Zeze.Gen.*;

public class Enum {
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

	private String Value;
	public final String getValue() {
		return Value;
	}
	private void setValue(String value) {
		Value = value;
	}
	private String Comment = "";
	public final String getComment() {
		return Comment;
	}
	private void setComment(String value) {
		Comment = value;
	}

	public Enum(XmlElement self) {
		setName(self.GetAttribute("name").strip());
		setValue(self.GetAttribute("value").strip());
		setComment(self.GetAttribute("description").strip());
		if (getComment().length() == 0) {
			setComment(self.GetAttribute("comment").strip());
			if (getComment().length() == 0) {
				XmlNode c = self.NextSibling;
				if (c != null && XmlNodeType.Text == c.NodeType) {
					setComment(c.InnerText.strip());
					Regex regex = new Regex("[\r\n]");
					setComment(regex.Replace(getComment(), ""));
				}
			}
		}
		if (getComment().length() > 0) {
			setComment(" // " + getComment());
		}
	}
}