using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.Types
{
    public class TypeDynamic : Type
    {
        public override string Name => "dynamic";

        public override bool IsImmutable => false;
        public override bool IsBean => false; // 虽然实际上包含其他bean，但自己不是。
        public override bool IsNeedNegativeCheck
        {
            get
            {
                foreach (var v in RealBeans)
                {
                    if (v.IsNeedNegativeCheck)
                        return true;
                }
                return false;
            }
        }

        public List<Bean> RealBeans { get; } = new List<Bean>(); // 在 Variable 里面已经去掉重复的了。

        public override void Accept(Visitor visitor)
        {
            visitor.Visit(this);
        }

        public override Type Compile(ModuleSpace space, string key, string value)
        {
            if (key != null && key.Length > 0)
                throw new Exception(Name + " type does not need a key. " + key);
            return new TypeDynamic(space, value);
        }

        private TypeDynamic(ModuleSpace space, string value)
        {
            foreach (string bean in value.Split(','))
            {
                if (bean.Length == 0)
                    continue;
                Type type = Type.Compile(space, bean, null, null);
                if (false == type.IsNormalBean)
                    throw new Exception("dynamic only support normal bean");
                RealBeans.Add((Bean)type);
            }
        }

        public override void Depends(HashSet<Type> includes)
        {
            if (includes.Add(this))
            {
                foreach (Bean bean in RealBeans)
                    bean.Depends(includes);
            }
        }

        internal TypeDynamic(SortedDictionary<String, Type> types)
        {
            types.Add(Name, this);
        }
    }
}
