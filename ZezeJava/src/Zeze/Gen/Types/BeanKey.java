package Zeze.Gen.Types;

import Zeze.*;
import Zeze.Gen.*;
import java.util.*;

public class BeanKey extends Type {
	@Override
	public void Accept(Visitor visitor) {
		visitor.Visit(this);
	}

	@Override
	public Type Compile(ModuleSpace space, String key, String value) {
		if (key != null && key.length() > 0) {
			throw new RuntimeException(getName() + " type does not need a key. " + key);
		}
		if (value != null && value.length() > 0) {
			throw new RuntimeException(getName() + " type does not need a value. " + value);
		}

		return this;
	}

	@Override
	public void Depends(HashSet<Type> includes) {
		if (includes.add(this)) {
			for (Variable var : getVariables()) {
				var.getVariableType().Depends(includes);
			}
		}
	}

	public final void Add(Variable var) {
		getVariables().add(var); // check duplicate
	}
	public final void Add(Enum e) {
		getEnums().add(e); // check duplicate
	}

	private ModuleSpace Space;
	public final ModuleSpace getSpace() {
		return Space;
	}
	private void setSpace(ModuleSpace value) {
		Space = value;
	}

	@Override
	public boolean isImmutable() {
		return true;
	}
	@Override
	public boolean isKeyable() {
		return true;
	}
	@Override
	public boolean isBean() {
		return true;
	}
	@Override
	public String getName() {
		return _name;
	}
	public final String getNamePinyin() {
		return Program.ToPinyin(getName());
	}
	private String _name;
	@Override
	public boolean isNeedNegativeCheck() {
		for (var v : getVariables()) {
			if (v.getVariableType().isNeedNegativeCheck()) {
				return true;
			}
		}
		return false;
	}


	private ArrayList<Variable> Variables = new ArrayList<Variable> ();
	public final ArrayList<Variable> getVariables() {
		return Variables;
	}
	private void setVariables(ArrayList<Variable> value) {
		Variables = value;
	}
	private ArrayList<Enum> Enums = new ArrayList<Enum> ();
	public final ArrayList<Enum> getEnums() {
		return Enums;
	}
	private void setEnums(ArrayList<Enum> value) {
		Enums = value;
	}
	private String Comment;
	public final String getComment() {
		return Comment;
	}
	private void setComment(String value) {
		Comment = value;
	}
	public final String getFullName() {
		return getSpace().Path(".", getName());
	}
	private long TypeId;
	public final long getTypeId() {
		return TypeId;
	}
	private void setTypeId(long value) {
		TypeId = value;
	}

	// ///////////////////////////////////////////
	public BeanKey(ModuleSpace space, XmlElement self) {
		setSpace(space);
		_name = self.GetAttribute("name").strip();
		Program.CheckReserveName(_name);
		Type.Add(space, this);
		space.Add(this);

		String attr = self.GetAttribute("TypeId");
		setTypeId(attr.length() > 0 ? Integer.parseInt(attr) : Zeze.Transaction.Bean.Hash64(space.Path(".", _name)));
		if (false == Program.getBeanTypeIdDuplicateChecker().add(getTypeId())) {
			throw new RuntimeException("duplicate Bean.TypeId, please choice one.");
		}

		parse(self);
	}

	private void parse(XmlElement self) {
		// previous sibling comment
		setComment(self.GetAttribute("comment"));
		if (getComment().length() == 0) {
			for (XmlNode c = self.PreviousSibling; null != c; c = c.PreviousSibling) {
				if (XmlNodeType.Element == c.NodeType) {
					break;
				}
				if (XmlNodeType.Comment == c.NodeType) {
					setComment(c.InnerText.strip());
					break;
				}
			}
		}

		XmlNodeList childNodes = self.ChildNodes;
		for (XmlNode node : childNodes) {
			if (XmlNodeType.Element != node.NodeType) {
				continue;
			}

			XmlElement e = (XmlElement)node;

			String nodename = e.Name;
			switch (e.Name) {
				case "variable":
					Add(new Variable(this, e));
					break;
				case "enum":
					Add(new Enum(e));
					break;
				default:
					throw new RuntimeException("node=" + nodename);
			}
		}
	}

	public final void Compile() {
		for (Variable var : getVariables()) {
			var.Compile(getSpace());
		}
		for (Variable var : getVariables()) {
			if (false == var.getVariableType().isKeyable()) {
				throw new RuntimeException("BeanKey need isKeyable variable. " + getSpace().Path(".", getName()) + "." + var.getName());
			}
		}
		// this.comparable = _isComparable();
	}
}