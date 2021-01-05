using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.IO;

namespace ConfigEditor.Gen.ts
{
    public class BeanFormatter
    {
        public static void Gen(string srcHome, Document doc, Property.DataOutputFlags flags)
        {
            using (StreamWriter sw = doc.OpenStreamWriter(srcHome, ".ts"))
            {
                sw.WriteLine($"// auto generated.");
                sw.WriteLine();
                Gen(sw, doc, doc.BeanDefine, flags);
                sw.WriteLine();

                var beanFullName = doc.BeanDefine.FullName().Replace('.', '_');

                sw.WriteLine($"export class {beanFullName} {{");
                sw.WriteLine($"    public static Beans: Array<_{beanFullName}> = new Array<_{beanFullName}>();");
                if (false == doc.Main.PropertyManager.Properties.TryGetValue(Property.Id.PName, out var pid))
                    throw new Exception("Property.Id miss!");
                foreach (var var in doc.BeanDefine.Variables)
                {
                    if (false == var.IsKeyable())
                        continue;
                    if (false == var.PropertiesList.Contains(pid))
                        continue;
                    var key = TypeHelper.GetName(var);
                    var value = beanFullName;
                    sw.WriteLine($"    public static BeansMap{var.Name}: Map<{key}, _{value}> = new Map<{key}, _{value}>();");
                }
                sw.WriteLine();
                sw.WriteLine($"    public static Load(): void");
                sw.WriteLine($"    {{");
                foreach (var bean in doc.Beans)
                {
                    sw.Write($"        var bean{bean.RowIndex}: _{beanFullName} = ");
                    GenLoad(sw, "        ", doc.BeanDefine, bean, flags);
                    sw.WriteLine($";");
                    sw.WriteLine($"        {beanFullName}.Beans.push(bean{bean.RowIndex});");
                    foreach (var var in doc.BeanDefine.Variables)
                    {
                        if (false == var.IsKeyable())
                            continue;
                        if (false == var.PropertiesList.Contains(pid))
                            continue;
                        sw.WriteLine($"        {beanFullName}.BeansMap{var.Name}.set(bean{bean.RowIndex}.V{var.Name}, bean{bean.RowIndex});");
                        sw.WriteLine();
                    }
                }
                sw.WriteLine($"    }}");
                sw.WriteLine($"}}");
            }
        }

        public static void GenLoad(StreamWriter sw, string prefix, BeanDefine beanDefine, Bean bean, Property.DataOutputFlags flags)
        {
            sw.WriteLine($"{{");
            foreach (var varDefine in beanDefine.Variables)
            {
                if (0 == (varDefine.DataOutputFlags & flags))
                    continue;
                if (bean.VariableMap.TryGetValue(varDefine.Name, out var varData))
                {
                    GenLoad(sw, prefix + "    ", varDefine, varData, flags);
                }
                else if (VarDefine.EType.List == varDefine.TypeNow)
                {
                    sw.WriteLine($"{prefix}    V{varDefine.Name}: [");
                    sw.WriteLine($"{prefix}    ],");
                }
            }
            sw.Write($"{prefix}}}");

        }

        public static void GenLoad(StreamWriter sw, string prefix, VarDefine varDefine, Bean.VarData varData, Property.DataOutputFlags flags)
        {
            switch (varDefine.TypeNow)
            {
                case VarDefine.EType.Date:
                    if (false == string.IsNullOrEmpty(varData.Value))
                        sw.WriteLine($"{prefix}V{varDefine.Name}: new Date(\"{varData.Value}\"),");
                    break;

                case VarDefine.EType.Double:
                    if (false == string.IsNullOrEmpty(varData.Value))
                        sw.WriteLine($"{prefix}V{varDefine.Name}: {varData.Value},");
                    break;

                case VarDefine.EType.Enum:
                    break; // TODO

                case VarDefine.EType.Float:
                    if (false == string.IsNullOrEmpty(varData.Value))
                        sw.WriteLine($"{prefix}V{varDefine.Name}: {varData.Value},");
                    break;

                case VarDefine.EType.Int:
                    if (false == string.IsNullOrEmpty(varData.Value))
                        sw.WriteLine($"{prefix}V{varDefine.Name}: {varData.Value},");
                    break;

                case VarDefine.EType.Long:
                    var strValue = (null == varData.Value) ? "" : varData.Value;
                    sw.WriteLine($"{prefix}V{varDefine.Name}: \"{strValue}\",");
                    break;

                case VarDefine.EType.List:
                    sw.WriteLine($"{prefix}V{varDefine.Name}: [");
                    foreach (var bean in varData.Beans)
                    {
                        sw.Write(prefix + "    ");
                        GenLoad(sw, prefix + "    ", varDefine.Reference, bean, flags);
                        sw.WriteLine(",");
                    }
                    sw.WriteLine($"{prefix}],");
                    break;

            }
        }

        public static void Gen(StreamWriter sw, Document doc, BeanDefine bean, Property.DataOutputFlags flags)
        {
            var beanFullName = bean.FullName().Replace('.', '_');

            sw.WriteLine($"export class _{beanFullName} {{");
            if (false == doc.Main.PropertyManager.Properties.TryGetValue(Property.IdList.PName, out var pid))
                throw new Exception("Property.Id miss!");

            // var property
            foreach (var var in bean.Variables)
            {
                if (0 == (var.DataOutputFlags & flags))
                    continue;
                var typeName = TypeHelper.GetName(var);
                if (var.Type != VarDefine.EType.List)
                {
                    sw.WriteLine($"    public V{var.Name}?: {typeName};");
                }
                else
                {
                    sw.WriteLine($"    private _V{var.Name}?: {typeName};");
                    sw.WriteLine($"    public get V{var.Name}() {{");
                    sw.WriteLine($"        return this._V{var.Name};");
                    sw.WriteLine($"    }}");
                    sw.WriteLine($"    public set V{var.Name}(value: {typeName}) {{");
                    sw.WriteLine($"        this._V{var.Name} = value;");
                    sw.WriteLine($"        for (var i = 0; i < value.length; ++i) {{");
                    foreach (var varRef in var.Reference.Variables)
                    {
                        if (false == varRef.IsKeyable())
                            continue;
                        if (false == varRef.PropertiesList.Contains(pid))
                            continue;
                        var key = TypeHelper.GetName(varRef);
                        var value = var.Reference.FullName().Replace('.', '_');
                        sw.WriteLine($"            var bean = value[i];");
                        sw.WriteLine($"            this.V{var.Name}Map{varRef.Name}.set(bean.V{varRef.Name}, bean);");
                    }
                    sw.WriteLine($"        }}");
                    sw.WriteLine($"    }}");
                    foreach (var varRef in var.Reference.Variables)
                    {
                        if (false == varRef.IsKeyable())
                            continue;
                        if (false == varRef.PropertiesList.Contains(pid))
                            continue;
                        var key = TypeHelper.GetName(varRef);
                        var value = var.Reference.FullName().Replace('.', '_');
                        sw.WriteLine($"    public V{var.Name}Map{varRef.Name}?: Map<{key}, _{value}> = new Map<{key}, _{value}>();");
                    }
                }
            }
            sw.WriteLine($"}}"); // end of bean
            sw.WriteLine();

            // sub bean
            foreach (var sub in bean.BeanDefines.Values)
            {
                Gen(sw, doc, sub, flags);
            }

        }
    }
}
