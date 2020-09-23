
using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen.Types
{
	public class Bean : Type
	{
		public override void Accept(Visitor visitor)
		{
			visitor.Visit(this);
		}

		public override Type Compile(ModuleSpace space, String key, String value)
		{
			if (key != null && key.Length > 0)
				throw new Exception(Name + " type does not need a key. " + key);
			if (value != null && value.Length > 0)
				throw new Exception(Name + " type does not need a value. " + value);

			return this;
		}

		public override void Depends(HashSet<Type> includes)
		{
			if (includes.Add(this))
				foreach (Variable var in Variables)
				{
					var.VariableType.Depends(includes);

					// 常量初始化引用到的Bean也加入depends中。 BeanName.ConstStaticVarName
					// 
					String[] initial = var.Initial.Split(".");
					String beanNameMabe = "";
					for (int i = 0; i < initial.Length - 1; ++i)
					{
						if (i > 0)
							beanNameMabe += '.';
						beanNameMabe += initial[i];
					}
					if (beanNameMabe.Length == 0)
						continue;
					try
					{
						Type type = Type.Compile(Space, beanNameMabe);
						if (null != type)
							includes.Add(type); // type.depends(type); 肯定是 Bean，不需要递归包含。 
					}
					catch (Exception)
					{
						// skip depends error
					}
				}
		}

		// ///////////////////////////////////////////
		//	// dynamic create
		//	Bean(ModuleSpace space, String name) {
		//		Type.Add(space, this);
		//	}
		//
		public void Add(Variable var)
		{
			Variables.Add(var); // check duplicate
		}
		public void Add(Enum e)
		{
			Enums.Add(e); // check duplicate
		}

		public ModuleSpace Space { get; private set; }

		public override bool IsImmutable => false;
		public override bool IsBean => true;
		public override string Name => _name;
        private string _name;
        public override bool IsNeedNegativeCheck
		{
			get
			{
				foreach (var v in Variables)
				{
					if (v.VariableType.IsNeedNegativeCheck)
						return true;
				}
				return false;
			}
		}
        public List<Variable> Variables { get; private set; } = new List<Variable>();
		public List<Enum> Enums { get; private set; } = new List<Enum>();
		public String Comment { get; private set; }
		public String FullName => Space.Path(".", Name);

		// ///////////////////////////////////////////
		public Bean(ModuleSpace space, XmlElement self)
		{
			Space = space;
			_name = self.GetAttribute("name").Trim();
			Type.Add(space, this);
			space.Add(this);

			parse(self);
		}

		private void parse(XmlElement self)
		{
			// previous sibling comment
			Comment = self.GetAttribute("comment");
			if (Comment.Length == 0)
			{
				for (XmlNode c = self.PreviousSibling; null != c; c = c.PreviousSibling)
				{
					if (XmlNodeType.Element == c.NodeType)
						break;
					if (XmlNodeType.Comment == c.NodeType)
					{
						Comment = c.InnerText.Trim();
						break;
					}
				}
			}

			XmlNodeList childNodes = self.ChildNodes;
			foreach (XmlNode node in childNodes)
			{
				if (XmlNodeType.Element != node.NodeType)
					continue;

				XmlElement e = (XmlElement)node;

				String nodename = e.Name;
				switch (e.Name)
				{
					case "variable":
						Add(new Variable(this, e));
						break;
					case "enum":
						Add(new Enum(e));
						break;
					default:
						throw new Exception("node=" + nodename);
				}
			}
		}

		public void Compile()
		{
			foreach (Variable var in Variables)
				var.Compile(Space);
			// this.comparable = _isComparable();
		}
	}
}
