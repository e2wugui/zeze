
using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen.Types
{
	public class BeanKey : Type
	{
		public override void Accept(Visitor visitor)
		{
			visitor.Visit(this);
		}

		public override Type Compile(ModuleSpace space, string key, string value, Variable var)
		{
			if (key != null && key.Length > 0)
				throw new Exception(Name + " type does not need a key. " + key);
			if (value != null && value.Length > 0)
				throw new Exception(Name + " type does not need a value. " + value);

			return this;
		}

		public override void DetectCircle(HashSet<Type> circle)
		{
			if (circle.Contains(this))
				throw new Exception($"DetectCircle @{FullName}");
			circle.Add(this);

			foreach (var v in Variables)
			{
				v.VariableType.DetectCircle(circle);
			}
		}

		public override void Depends(HashSet<Type> includes)
		{
			if (includes.Add(this))
            {
				foreach (Variable var in Variables)
				{
					var.VariableType.Depends(includes);
				}
			}
		}

		public void Add(Variable var)
		{
			foreach (var vv in Variables)
			{
				if (vv.Id.Equals(var.Id))
					throw new ArgumentException("duplicate Variable.Id: " + var.Id);
				if (vv.Name.Equals(var.Name))
					throw new ArgumentException("duplicate Variable.Name: " + var.Name);
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

		public override bool IsImmutable => true;
		public override bool IsKeyable => true;
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
		public BeanKey(ModuleSpace space, XmlElement self)
		{
			Space = space;
			_name = self.GetAttribute("name").Trim();
			Kind = "beankey";
			Program.CheckReserveName(_name);
			Type.Add(space, this);
			space.Add(this);

			string attr = self.GetAttribute("TypeId");
			TypeId = attr.Length > 0 ? int.Parse(attr) : Zeze.Transaction.Bean.Hash64(space.Path(".", _name));
			if (false == Program.BeanTypeIdDuplicateChecker.Add(TypeId))
				throw new Exception("duplicate Bean.TypeId, please choice one.");

			parse(self);
		}

		private void parse(XmlElement self)
		{
			// previous sibling comment
			Comment = self.GetAttribute("comment");
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
            {
				var.Compile(Space);
			}
			foreach (Variable var in Variables)
			{
				if (false == var.VariableType.IsKeyable)
					throw new Exception("BeanKey need isKeyable variable. " + Space.Path(".", Name) + "." + var.Name);
			}
			// this.comparable = _isComparable();
		}
	}
}
