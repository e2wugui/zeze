using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.lua
{
    public class BeanFormatter
    {
        public static void Make(string moduleName, string beanName, long beanTypeId,
            List<Types.Variable> vars, List<Types.Enum> enums,
            System.IO.StreamWriter sw)
        {
            sw.WriteLine($"{moduleName}.{beanName} = {{");
            sw.WriteLine($"    _TypeId_ = {beanTypeId},");
            foreach (var v in vars)
            {
                sw.WriteLine($"    {v.Name} = {v.Id},");
            }
            foreach (var e in enums)
            {
                if (double.TryParse(e.Value, out var _)) // is number
                {
                    sw.WriteLine($"    {e.NamePinyin} = {e.Value},");
                }
                else
                {
                    sw.WriteLine($"    {e.NamePinyin} = \"{e.Value}\",");
                }
            }
            sw.WriteLine("}");
        }

        public static void MakeMeta(string beanFullName, long typeId,
            List<Types.Variable> vars,
            System.IO.StreamWriter sw)
        {
            sw.WriteLine("meta.beans[" + typeId + "] = {");
            sw.WriteLine($"    [0] = \"{beanFullName}\", ");
            foreach (var v in vars)
            {
                // 生成动态Bean的类型（TypeId）常量。
                if (v.VariableType is Types.TypeDynamic d)
                {
                    foreach (var real in d.RealBeans)
                    {
                        sw.WriteLine($"    [\"DynamicTypeId_{v.NameUpper1}_{real.Value.Space.Path("_", real.Value.Name)}\"] = {real.Key},");
                    }
                    if (d.RealBeans.Count > 0)
                        sw.WriteLine();
                }

                sw.WriteLine($"    [{v.Id}] = {TypeMeta.Get(v, v.VariableType)},");
            }
            sw.WriteLine("}");
            foreach (var v in vars)
            {
                if (v.VariableType is Types.TypeDynamic d)
                {
                    sw.WriteLine($"function Zeze_GetRealBeanTypeIdFromSpecial_{beanFullName}_{v.NamePinyin}(specialTypeId)");
                    foreach (var r in d.RealBeans)
                    {
                        sw.WriteLine($"    if (specialTypeId == {r.Key}) then");
                        sw.WriteLine($"        return {r.Value.TypeId}");
                        sw.WriteLine($"    end");
                    }
                    sw.WriteLine($"    return specialTypeId");
                    sw.WriteLine($"end");
                }
            }
        }
    }
}
