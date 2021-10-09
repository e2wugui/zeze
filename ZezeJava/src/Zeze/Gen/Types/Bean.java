package Zeze.Gen.Types;

import Zeze.*;
import Zeze.Gen.*;
import java.util.*;

public class Bean extends Type {
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

				// 常量初始化引用到的Bean也加入depends中。 BeanName.ConstStaticVarName
				// 
				String[] initial = var.getInitial().split("[.]", -1);
				String beanNameMabe = "";
				for (int i = 0; i < initial.length - 1; ++i) {
					if (i > 0) {
						beanNameMabe += '.';
					}
					beanNameMabe += initial[i];
				}
				if (beanNameMabe.length() == 0) {
					continue;
				}
				try {
					Type type = Type.Compile(getSpace(), beanNameMabe);
					if (null != type) {
						includes.add(type); // type.depends(type); 肯定是 Bean，不需要递归包含。
					}
				}
				catch (RuntimeException e) {
					// skip depends error
				}
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
		return false;
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
	public Bean(ModuleSpace space, XmlElement self) {
		setSpace(space);
		_name = self.GetAttribute("name").strip();
		Program.CheckReserveName(_name);
		Type.Add(space, this);
		space.Add(this);

		// previous sibling comment
		setComment(self.GetAttribute("comment"));
		String attr = self.GetAttribute("TypeId");
		setTypeId(attr.length() > 0 ? Integer.parseInt(attr) : Zeze.Transaction.Bean.Hash64(space.Path(".", _name)));
		if (false == Program.getBeanTypeIdDuplicateChecker().add(getTypeId())) {
			throw new RuntimeException("duplicate Bean.TypeId, please choice one.");
		}

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
		// this.comparable = _isComparable();
	}
}