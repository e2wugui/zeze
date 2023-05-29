using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.Types
{
    public class TypeDynamic : Type
    {
        public override string Name => "dynamic";

        public override bool IsImmutable => false;
        public override bool IsJavaPrimitive => false;
        public override bool IsNeedNegativeCheck
        {
            get
            {
                foreach (var v in RealBeans.Values)
                {
                    if (v.IsNeedNegativeCheck)
                        return true;
                }
                return false;
            }
        }

        public SortedDictionary<long, Bean> RealBeans { get; } = new SortedDictionary<long, Bean>();
        public int SpecialCount { get; }
        public DynamicParams DynamicParams => Variable.DynamicParams;

        public override void Accept(Visitor visitor)
        {
            visitor.Visit(this);
        }

        public override Type Compile(ModuleSpace space, string key, string value, Variable var)
        {
            if (key != null && key.Length > 0)
                throw new Exception(Name + " type does not need a key. " + key);
            return new TypeDynamic(space, var);
        }

        // value=BeanName[:SpecialTypeId],BeanName2[:SpecialTypeId2]
        // 如果指定特别的TypeId，必须全部都指定。虽然部分指定也可以处理，感觉这样不大好。
        private TypeDynamic(ModuleSpace space, Variable var)
        {
            Kind = "dynamic";
            Variable = var;
            if (DynamicParams.Beans.Count == 0)
            {
                if (false == string.IsNullOrEmpty(DynamicParams.Base)
                    && Program.NamedObjects.TryGetValue(DynamicParams.Base.ToLower(), out var baseType)
                    && baseType is Bean baseBean)
                {
                    DynamicParams.Beans.Add(baseBean.FullName);
                    foreach (var derive in baseBean.Derives)
                        DynamicParams.Beans.Add(derive);
                }
            }
            foreach (var beanWithSpecialTypeId in DynamicParams.Beans)
            {
                if (beanWithSpecialTypeId.Length == 0) // empty
                    continue;
                var beanWithSpecialTypeIdArray = beanWithSpecialTypeId.Split(':');
                if (beanWithSpecialTypeIdArray.Length == 0)
                    continue;
                Type type = Type.Compile(space, beanWithSpecialTypeIdArray[0], null, null, null);
                if (false == type.IsNormalBean)
                    throw new Exception("dynamic only support normal bean");
                Bean bean = type as Bean;
                long specialTypeId = bean.TypeId; // default
                if (beanWithSpecialTypeIdArray.Length > 1)
                {
                    SpecialCount++;
                    specialTypeId = long.Parse(beanWithSpecialTypeIdArray[1]);
                    if (specialTypeId <= 0)
                        throw new Exception("SpecialTypeId <= 0 is reserved");
                }
                RealBeans.Add(specialTypeId, bean);
            }

            if (SpecialCount == 0) // 没有配置特别的TypeId，全部使用Bean本身的TypeId。
                return;

            if (RealBeans.Count == 0) // 动态类型没有配置任何具体的Bean。允许。
                return;

            if (SpecialCount != RealBeans.Count)
            {
                throw new Exception("dynamic setup special TypeId，But Not All.");
            }
        }

        public override void Depends(HashSet<Type> includes, string parent)
        {
            if (includes.Add(this))
            {
                if (parent != null)
                    parent += ".DynamicBean";
                foreach (var bean in RealBeans.Values)
                    bean.Depends(includes, parent);
            }
        }

        public override void DependsIncludesNoRecursive(HashSet<Type> includes)
        {
            if (includes.Add(this))
            {
                foreach (var bean in RealBeans.Values)
                    bean.DependsIncludesNoRecursive(includes);
            }
        }

        internal TypeDynamic(SortedDictionary<string, Type> types)
        {
            types.Add(Name, this);
        }
    }
}
