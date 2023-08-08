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

		public void DependsVariables(HashSet<Type> includes)
		{
            foreach (Variable var in Variables)
            {
                var.VariableType.DependsIncludesNoRecursive(includes);
                DependsInitial(var, includes);
            }
        }

        public override void DependsIncludesNoRecursive(HashSet<Type> includes)
		{
			includes.Add(this);
        }

        public override void Depends(HashSet<Type> includes, string parent)
		{
			if (includes.Add(this))
			{
				if (parent != null)
				{
					parent += ".Bean(" + FullName + ')';
					Console.WriteLine("Depends: " + parent);
				}
				foreach (Variable var in Variables)
                {
                    var.VariableType.Depends(includes, parent != null ? parent + ".Var(" + var.Name + ')' : null);
                    DependsInitial(var, includes);
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

		public ModuleSpace Space { get; protected set; }

		public override bool IsImmutable => false;
		public override bool IsJavaPrimitive => false;
		public override string Name => _name;
		public string NamePinyin => Program.ToPinyin(Name);
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
		public virtual string FullCxxName => Space.Path("::", Name);
		public long TypeId { get; private set; }
		public bool Extendable { get; private set; }
		public bool Equalable { get; private set; }
		public string Base { get; private set; }
		public List<string> Derives = new();
		public string Version { get; private set; }
		public bool MappingClass { get; private set; }
		public bool UseData { get; private set; }
		public bool CustomTypeId { get; private set; }

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
		public bool Hot { get; private set; } = false;

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

		protected Bean()
		{
        }

		public string GetBaseHotName(string name)
		{
			var refName = name;
			TryParseHotName(ref refName);
			return refName;
		}

        public int TryParseHotName(ref string name)
		{
			if (false == Hot)
				return 0;

			if (!name.EndsWith("_"))
				throw new Exception($"invalid hot name {name}");

			// reverse find
			var versionEnd = name.Length - 1;
            for (; versionEnd >= 0; versionEnd--)
			{
				if (name[versionEnd] == '_')
					continue; // 忽略结束的下划线，现在的写法允许多个结束的下划线。
			}

			var versionBegin = versionEnd;
			for (; versionBegin >= 0; versionBegin--)
			{
				if (name[versionBegin] == '_')
					break;
			}
			if (versionBegin < 0)
				throw new Exception($"invalid hot name {name}");

			name = name.Substring(0, versionBegin);
			versionBegin++;
			var version = int.Parse(name.Substring(versionBegin, versionEnd - versionBegin + 1));
			if (version <= 0)
				throw new Exception($"invalid hot name {name} version must great than 0.");
			return version;
		}

        // ///////////////////////////////////////////
        public Bean(ModuleSpace space, XmlElement self)
		{
			Space = space;
			_name = self.GetAttribute("name").Trim();
			Kind = self.GetAttribute("kind").Trim();
			if (string.IsNullOrEmpty(Kind))
				Kind = "bean"; // default
			Hot = self.GetAttribute("hot").Equals("true");
			Program.CheckReserveName(_name, space.Path(), Hot);
			Type.Add(space, this);
			space.Add(this);

			string attr = self.GetAttribute("TypeId");
			Extendable = self.GetAttribute("extendable") == "true";
			Equalable = space.Equalable || self.GetAttribute("equals") == "true";
			Base = self.GetAttribute("base");
			if (Base != "" && !Base.Contains('.'))
				Base = Space.Path(".", Base);
			var hashTypeId = Util.FixedHash.Hash64(space.Path(".", GetBaseHotName(_name)));
			// 这里的写法：hot bean 允许自定义TypeId，
			// 但是java的Bean禁止了自定义功能，
			// 而Hot目前仅用于java，
			// 所以实际上还是不准自定义的。
			TypeId = attr.Length > 0 ? int.Parse(attr) : hashTypeId;
			CustomTypeId = TypeId != hashTypeId;
			if (false == Program.BeanTypeIdDuplicateChecker.Add(TypeId))
				throw new Exception("duplicate Bean.TypeId, please choice one.");
			Comment = GetComment(self);
			Version = self.GetAttribute("version").Trim();
			MappingClass = self.GetAttribute("MappingClass").Equals("true");
			if (MappingClass)
				space.AddMappingClassBean(this);
			UseData = self.GetAttribute("UseData") switch
			{
				"true" => true,
				"false" => false,
				_ => space.UseData
			};

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

		public static string Trim(string str)
		{
			var s = str.Trim();
			if (s.Length == 0)
				return "";
			str = str.Replace("\r", "");
			if (str.Contains('\n'))
			{
				int i, j;
				for (i = 0;; i++)
				{
					if (!char.IsWhiteSpace(str[i]))
						break;
				}
				for (j = i - 1; j >= 0; j--)
				{
					if (str[j] == '\n')
						break;
				}
				if (j < 0)
					j = i - 1;
				str = str[(j + 1)..].TrimEnd();
			}
			else
				str = str.Trim();
			return str;
		}
		
		public static string GetComment(XmlElement self)
		{
			var comment = self.GetAttribute("comment").Trim();
			if (comment.Length == 0)
			{
				var cn = self.ChildNodes;
				if (cn.Count > 0)
				{
					if (cn[0]?.NodeType == XmlNodeType.Text)
						comment = cn[0].InnerText;
				}
				else
				{
					var xn = self.NextSibling;
					if (xn?.NodeType == XmlNodeType.Text)
						comment = xn.InnerText;
				}
				if (comment.Length > 0)
				{
					var p = comment.IndexOf('\n');
					if (p >= 0)
						comment = comment[..p];
					comment = comment.Trim();
				}
				if (comment.Length == 0)
				{
					for (var c = self.PreviousSibling; c != null; c = c.PreviousSibling)
					{
						if (c.NodeType == XmlNodeType.Element)
							break;
						if (c.NodeType == XmlNodeType.Comment)
						{
							comment = Trim(c.InnerText);
							break;
						}
					}
				}
			}

			if (comment.Length > 0)
			{
				if (comment.Contains('\n'))
					comment = "/*\n" + comment + "\n*/";
				else
					comment = "// " + comment;
			}

			return comment;
		}

		public Variable GetVariable(string name)
		{
			foreach (var v in Variables)
				if (v.Name.Equals(name))
					return v;
			return null;
		}

		public void Compile()
		{
			foreach (Variable var in Variables)
				var.Compile(Space);

			if (false == string.IsNullOrEmpty(Version))
			{
				var v = GetVariable(Version);
				if (null == v)
					throw new Exception($"version var not found. Bean={FullName} version={Version}");
				if (!(v.VariableType is TypeLong))
					throw new Exception($"type of version var is not long. Bean={FullName} version={Version}");
			}
			// this.comparable = _isComparable();
		}

		public Variable GetFirstDynamicVariable()
		{
            foreach (var var in Variables)
			{
                if (var.VariableType is TypeDynamic)
                    return var;
            }
            return null;
        }

        public bool RecursiveCheckDynamicCountLessThanOrEqual(int n)
		{
			var dynamicCount = 0;
            foreach (var var in Variables)
			{
				if (var.VariableType is TypeDynamic dVar)
				{
					++dynamicCount;
                    foreach (var bean in dVar.DynamicParams.Beans)
					{
                        var beanWithSpecialTypeIdArray = bean.Split(':');
                        var dBean = Type.Compile(Space, beanWithSpecialTypeIdArray[0]) as Bean;
						if (false == dBean.RecursiveCheckDynamicCountLessThanOrEqual(n))
							return false;
					}
				}

            }
			return dynamicCount <= 1;
        }

        public string MappingClassName(List<Types.Bean> inherits)
        {
			var sb = new StringBuilder();
			sb.Append("CMapping");
			for (int i = 0; i < inherits.Count; ++i)
				sb.Append(inherits[i].Name);
            return sb.ToString();
        }
    }
}
