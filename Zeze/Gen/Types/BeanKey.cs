
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

			circle.Remove(this);
		}

		public override void Depends(HashSet<Type> includes, string parent)
		{
			if (includes.Add(this))
            {
	            if (parent != null)
	            {
		            parent += ".BeanKey(" + FullName + ')';
		            Console.WriteLine("Depends: " + parent);
	            }
				foreach (Variable var in Variables)
				{
					var.VariableType.Depends(includes, parent != null ? parent + ".Var(" + var.Name + ')' : null);
				}
			}
		}

        public override void DependsIncludesNoRecursive(HashSet<Type> includes)
        {
			includes.Add(this);
        }

        public void DependsVariables(HashSet<Type> includes)
        {
            foreach (Variable var in Variables)
            {
                var.VariableType.DependsIncludesNoRecursive(includes);
                DependsInitial(var, includes);
            }
        }

        public void DependsInitial(Variable var, HashSet<Type> includes)
        {
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
                return; // done
            try
            {
                Type type = Type.Compile(Space, beanNameMabe);
                if (type != null)
                    includes.Add(type); // 不需要调用DependsXXX，肯定是Bean，直接加入。
            }
            catch (Exception ex)
            {
                // 这里为什么try-catch了，需要确认。
                Console.WriteLine(ex.ToString());
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

		public ModuleSpace Space { get; protected set; }

		public override bool IsImmutable => true;
		public override bool IsJavaPrimitive => false;
		public override bool IsKeyable => true;
		public override string Name => _name;
		public string NamePinyin => Program.ToPinyin(Name);
        public virtual string FullCxxName => Space.Path("::", Name);
        
		protected string _name;
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
		public virtual string FullName => Space.Path(".", Name);
		public long TypeId { get; private set; }
		
		private List<Variable> VariablesIdOrder_;
		public List<Variable> VariablesIdOrder
		{
			get
			{
				if (VariablesIdOrder_ == null)
				{
					var list = new List<Variable>();
					list.AddRange(Variables);
					list.Sort((a, b) => a.Id - b.Id);
					VariablesIdOrder_ = list;
				}
				return VariablesIdOrder_;
			}
		}

        // ///////////////////////////////////////////
        public static void BeautifulVariableId(XmlElement self)
        {
            XmlNodeList childNodes = self.ChildNodes;
            var varId = 1;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;

                switch (e.Name)
                {
                    case "variable":
                        Variable.BeautifulVariableId(e, varId++);
                        break;
                }
            }
        }
        
		public BeanKey()
		{

		}

		public BeanKey(ModuleSpace space, XmlElement self)
		{
			Space = space;
			_name = self.GetAttribute("name").Trim();
			Kind = "beankey";
			Program.CheckReserveName(_name, space.Path());
			Type.Add(space, this);
			space.Add(this);

			string attr = self.GetAttribute("TypeId");
			TypeId = attr.Length > 0 ? int.Parse(attr) : Util.FixedHash.Hash64(space.Path(".", _name));
			if (false == Program.BeanTypeIdDuplicateChecker.Add(TypeId))
				throw new Exception("duplicate Bean.TypeId, please choice one.");

			parse(self);
		}

		private void parse(XmlElement self)
		{
			Comment = Bean.GetComment(self);

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
