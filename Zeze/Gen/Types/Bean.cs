using System;
using System.Collections.Generic;
using System.Xml;

namespace Zeze.Gen.Types
{
	public class Bean : Type
	{
		public override void Accept(Visitor visitor)
		{
			visitor.Visit(this);
		}

		public override Type Compile(ModuleSpace space, string key, string value)
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
                    string[] initial = var.Initial.Split('.');
                    string beanNameMabe = "";
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
						if (type != null)
							includes.Add(type); // type.depends(type); 肯定是 Bean，不需要递归包含。 
					}
					catch (Exception ex)
					{
						// 这里为什么try-catch了，需要确认。
						Console.WriteLine(ex.ToString());
					}
				}
		}

		// 当这个Bean作为Map.Value定义时，把Map.KeyType加到这里。
		// RocksRaft 自动生成改KeyType的属性，用来保存Bean被加入Map时的Key的值。
		// Map.Value改变时，从Value中读取Key的值，快速找到对应的KV键值。
		public HashSet<Type> MapKeyTypes { get; } = new HashSet<Type>();

		public void Add(Variable var)
		{
			foreach (var vv in Variables)
            {
				if (vv.Id.Equals(var.Id))
					throw new ArgumentException($"duplicate Variable.Id: {var.Id} in {FullName}");
				if (vv.Name.Equals(var.Name))
					throw new ArgumentException($"duplicate Variable.Name: {var.Name} in {FullName}");
			}
			Variables.Add(var);
		}

		public void Add(Enum e)
		{
			foreach (var ee in Enums)
            {
				if (ee.Name.Equals(e.Name))
					throw new ArgumentException("duplicate Enum Name: " + e.Name);
            }
			Enums.Add(e);
		}

		public ModuleSpace Space { get; private set; }

		public override bool IsImmutable => false;
		public override string Name => _name;
		public string NamePinyin => Program.ToPinyin(Name);
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
		public string Comment { get; private set; }
		public string FullName => Space.Path(".", Name);
		public long TypeId { get; private set; }
		// ///////////////////////////////////////////
		public Bean(ModuleSpace space, XmlElement self)
		{
			Space = space;
			_name = self.GetAttribute("name").Trim();
			Kind = self.GetAttribute("kind").Trim();
			if (string.IsNullOrEmpty(Kind))
				Kind = "bean"; // default
			Program.CheckReserveName(_name);
			Type.Add(space, this);
			space.Add(this);

			// previous sibling comment
			Comment = self.GetAttribute("comment");
			string attr = self.GetAttribute("TypeId");
			TypeId = attr.Length > 0 ? int.Parse(attr) : Zeze.Transaction.Bean.Hash64(space.Path(".", _name));
			if (false == Program.BeanTypeIdDuplicateChecker.Add(TypeId))
				throw new Exception("duplicate Bean.TypeId, please choice one.");

			if (Comment.Length == 0)
			{
				for (XmlNode c = self.PreviousSibling; c != null; c = c.PreviousSibling)
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

                string nodename = e.Name;
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
