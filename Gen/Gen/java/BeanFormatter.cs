using System;
using System.IO;
using Zeze.Gen.Types;
using Enum = Zeze.Gen.Types.Enum;
using Type = Zeze.Gen.Types.Type;

namespace Zeze.Gen.java
{
    public class BeanFormatter
    {
        readonly Bean bean;

        public BeanFormatter(Bean bean)
        {
            this.bean = bean;
        }

        public void MakeRedirectResult(string baseDir, Project project)
        {
            using StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + ".java");
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + bean.Space.Path() + ";");
            sw.WriteLine();
            if (bean.Comment.Length > 0)
                sw.WriteLine(bean.Comment);
            sw.WriteLine($"public class {bean.Name} extends Zeze.Arch.RedirectResult {{");
            foreach (var v in bean.Variables)
            {
                sw.WriteLine($"    public {TypeName.GetName(v.VariableType)} {v.Name};{v.Comment}");
            }
            sw.WriteLine();
            ConstructRedirectResult.Make(bean, sw, "    ");
            sw.WriteLine("}");
        }

        public void MakeReadOnly(string baseDir)
        {
            using StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + "ReadOnly.java");
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + bean.Space.Path() + ";");
            sw.WriteLine();
            //sw.WriteLine("import Zeze.Serialize.ByteBuffer;");
            //sw.WriteLine();
            if (bean.Comment.Length > 0)
                sw.WriteLine(bean.Comment);
            sw.WriteLine($"public interface {bean.Name}ReadOnly {{");
            PropertyReadOnly.Make(bean, sw, "    ");
            sw.WriteLine("}");
        }

        public void Make(string baseDir, Project project)
        {
            if (bean.RedirectResult)
            {
                MakeRedirectResult(baseDir, project);
                return;
            }

            if (!Program.isOnlyData(bean))
                MakeReadOnly(baseDir);

            using StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + ".java");
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + bean.Space.Path() + ";");
            sw.WriteLine();
            sw.WriteLine("import Zeze.Serialize.ByteBuffer;");
            sw.WriteLine("import Zeze.Serialize.IByteBuffer;");
            sw.WriteLine();
            if (Program.isOnlyData(bean))
                new javadata.BeanFormatter(bean).Make(sw, bean.Name);
            else
            {
                if (bean.Comment.Length > 0)
                    sw.WriteLine(bean.Comment);
                var extraSuppress1 = bean.GenEquals ? "" : "\"EqualsAndHashcode\", ";
                var extraSuppress2 = Program.isData(bean) ? ", \"UnnecessarilyQualifiedInnerClassAccess\"" : "";
                var extraSuppress3 = bean.Interface == "" ? "" : ", \"override\"";
                sw.WriteLine($"@SuppressWarnings({{{extraSuppress1}\"NullableProblems\", \"RedundantIfStatement\", \"RedundantSuppression\", \"SuspiciousNameCombination\", \"SwitchStatementWithTooFewBranches\"{extraSuppress2}, \"UnusedAssignment\"{extraSuppress3}}})");
                var final = bean.Extendable ? "" : "final ";
                var extraInterface = bean.Interface == "" ? "" : ", " + bean.Interface;
                sw.WriteLine($"public {final}class {bean.Name} extends Zeze.Transaction.Bean implements {bean.Name}ReadOnly{extraInterface} {{");
                WriteDefine(sw, project);
                if (Program.isData(bean))
                {
                    sw.WriteLine();
                    new javadata.BeanFormatter(bean).Make(sw, "Data");
                }
                sw.WriteLine("}");
            }
        }

        string GetAndCreateDynamicBean(string beanName, int varId, TypeDynamic type)
        {
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId)) // 判断一个就够了。
            {
                return $"{beanName}::getSpecialTypeIdFromBean_{varId}, " +
                       $"{beanName}::createBeanFromSpecialTypeId_{varId}";
            }
            return $"{type.DynamicParams.GetSpecialTypeIdFromBean}, " +
                   $"{type.DynamicParams.CreateBeanFromSpecialTypeId}";
        }

        // 非dynamic集合变量的meta常量化：声明静态meta1/meta2/factory，让Construct（无参/全参构造器）
        // 走(Meta)/(Factory)构造器，消除每bean实例化的工厂缓存探测（bean表加载/对象池复用热路径）。
        // 初始化仍走公开工厂——Bean拦截与全局共享缓存语义不变，仅把工厂调用从每实例一次变为
        // 每类加载一次；同类型元组跨变量/跨bean共享同一meta实例（工厂缓存保证）。
        // dynamic值集合不在此列（createDynamic*带每bean专属的get/create函数，见GenDynamicSpecialMethod）。
        // 空行发在声明之后（与字段声明贴成一组），而非之前——与GenDynamicSpecialMethod的约定一致。
        private void GenCollectionMetaDefine(StreamWriter sw, string prefix, Variable var)
        {
            Type vt = var.VariableType;
            string varName = var.NamePrivate;
            if (vt is TypeCollection collection) // TypeList/TypeSet
            {
                string value = BoxingName.GetBoxingName(collection.ValueType);
                string factory = vt is TypeSet ? "getSet1Meta"
                        : collection.ValueType.IsNormalBean ? "getList2Meta" : "getList1Meta";
                sw.WriteLine($"{prefix}private static final Zeze.Transaction.Collections.Meta1<{value}> meta1{varName}");
                sw.WriteLine($"{prefix}        = Zeze.Transaction.Collections.Meta1.{factory}({value}.class);");
                sw.WriteLine();
            }
            else if (vt is TypeMap map)
            {
                string key = BoxingName.GetBoxingName(map.KeyType);
                string value = BoxingName.GetBoxingName(map.ValueType);
                string factory = map.ValueType.IsNormalBean ? "getMap2Meta" : "getMap1Meta";
                sw.WriteLine($"{prefix}private static final Zeze.Transaction.Collections.Meta2<{key}, {value}> meta2{varName}");
                sw.WriteLine($"{prefix}        = Zeze.Transaction.Collections.Meta2.{factory}({key}.class, {value}.class);");
                sw.WriteLine();
            }
            else if (vt is TypeSortedMap sortedMap)
            {
                string key = BoxingName.GetBoxingName(sortedMap.KeyType);
                string value = BoxingName.GetBoxingName(sortedMap.ValueType);
                string factory = sortedMap.ValueType.IsNormalBean ? "getSortedMap2Meta" : "getSortedMap1Meta";
                sw.WriteLine($"{prefix}private static final Zeze.Transaction.Collections.Meta2<{key}, {value}> meta2{varName}");
                sw.WriteLine($"{prefix}        = Zeze.Transaction.Collections.Meta2.{factory}({key}.class, {value}.class);");
                sw.WriteLine();
            }
            else if (vt is TypeGTable gtable)
            {
                string rowKey = BoxingName.GetBoxingName(gtable.RowKeyType);
                string colKey = BoxingName.GetBoxingName(gtable.ColKeyType);
                string value = BoxingName.GetBoxingName(gtable.ValueType);
                if (gtable.ValueType.IsNormalBean)
                {
                    sw.WriteLine($"{prefix}private static final Zeze.Transaction.GTable.GTable2.Factory<{rowKey}, {colKey}, {value}, {value}ReadOnly> factory{varName}");
                    sw.WriteLine($"{prefix}        = Zeze.Transaction.GTable.GTable2.getFactory({rowKey}.class, {colKey}.class, {value}.class);");
                }
                else
                {
                    sw.WriteLine($"{prefix}private static final Zeze.Transaction.GTable.GTable1.Factory<{rowKey}, {colKey}, {value}> factory{varName}");
                    sw.WriteLine($"{prefix}        = Zeze.Transaction.GTable.GTable1.getFactory({rowKey}.class, {colKey}.class, {value}.class);");
                }
                sw.WriteLine();
            }
        }

        private void GenDynamicSpecialMethod(StreamWriter sw, string prefix, Variable var, TypeDynamic type, bool isCollection)
        {
            if (false == isCollection)
            {
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine();
                    sw.WriteLine($"{prefix}public static final long DynamicTypeId_{var.NameUpper1}_{real.Value.Space.Path("_", real.Value.Name)} = {real.Key}L;");
                }
            }
            else
            {
                var vt = var.VariableType;
                // meta声明与字段声明贴成一组（空行在其后，由下方统一发），与GenCollectionMetaDefine约定一致。
                if (vt is TypeCollection)
                {
                    sw.WriteLine($"{prefix}private static final Zeze.Transaction.Collections.Meta1<Zeze.Transaction.DynamicBean> meta1{var.NamePrivate}");
                    sw.WriteLine($"{prefix}        = Zeze.Transaction.Collections.Meta1.createDynamicListMeta({GetAndCreateDynamicBean(bean.Name, var.Id, type)});");
                }
                else if (vt is TypeMap map)
                {
                    sw.WriteLine($"{prefix}private static final Zeze.Transaction.Collections.Meta2<{BoxingName.GetBoxingName(map.KeyType)}, Zeze.Transaction.DynamicBean> meta2{var.NamePrivate}");
                    sw.WriteLine($"{prefix}        = Zeze.Transaction.Collections.Meta2.createDynamicMapMeta({BoxingName.GetBoxingName(map.KeyType)}.class, {GetAndCreateDynamicBean(bean.Name, var.Id, type)});");
                }
                else if (vt is TypeSortedMap smap)
                {
                    sw.WriteLine($"{prefix}private static final Zeze.Transaction.Collections.Meta2<{BoxingName.GetBoxingName(smap.KeyType)}, Zeze.Transaction.DynamicBean> meta2{var.NamePrivate}");
                    // sortedmap 必须用 createDynamicSortedMapMeta（sortedMap2 家族哈希），
                    // 与 PSortedMap2 动态构造器及读端注册对称；用 map2 家族会 typeId 永不匹配（FND3-04）。
                    sw.WriteLine($"{prefix}        = Zeze.Transaction.Collections.Meta2.createDynamicSortedMapMeta({BoxingName.GetBoxingName(smap.KeyType)}.class, {GetAndCreateDynamicBean(bean.Name, var.Id, type)});");
                }
            }
            sw.WriteLine();
            sw.WriteLine($"{prefix}public static Zeze.Transaction.DynamicBean newDynamicBean_{var.NameUpper1}() {{");
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId)) // 判断一个就够了。
                sw.WriteLine($"{prefix}    return new Zeze.Transaction.DynamicBean({var.Id}, {bean.Name}::getSpecialTypeIdFromBean_{var.Id}, {bean.Name}::createBeanFromSpecialTypeId_{var.Id});");
            else
                sw.WriteLine($"{prefix}    return new Zeze.Transaction.DynamicBean({var.Id}, {type.DynamicParams.GetSpecialTypeIdFromBean}, {type.DynamicParams.CreateBeanFromSpecialTypeId});");
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}public static long getSpecialTypeIdFromBean_{var.Id}(Zeze.Transaction.Bean _b_) {{");
            if (string.IsNullOrEmpty(type.DynamicParams.GetSpecialTypeIdFromBean))
            {
                // 根据配置的实际类型生成switch。
                sw.WriteLine($"{prefix}    var _t_ = _b_.typeId();");
                sw.WriteLine($"{prefix}    if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)");
                sw.WriteLine($"{prefix}        return Zeze.Transaction.EmptyBean.TYPEID;");
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}    if (_t_ == {real.Value.TypeId}L)");
                    sw.WriteLine($"{prefix}        return {real.Key}L; // {real.Value.FullName}");
                }
                sw.WriteLine($"{prefix}    throw new UnsupportedOperationException(\"Unknown Bean! dynamic@{((Bean)var.Bean).FullName}:{var.Name}:\" + _t_);");
            }
            else
            {
                // 转发给全局静态（static）函数。
                sw.WriteLine($"{prefix}    return {type.DynamicParams.GetSpecialTypeIdFromBean.Replace("::", ".")}(_b_);");
            }
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
            sw.WriteLine($"{prefix}public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_{var.Id}(long _t_) {{");
            //sw.WriteLine($"{prefix}    case Zeze.Transaction.EmptyBean.TYPEID: return new Zeze.Transaction.EmptyBean();");
            if (string.IsNullOrEmpty(type.DynamicParams.CreateBeanFromSpecialTypeId))
            {
                // 根据配置的实际类型生成switch。
                foreach (var real in type.RealBeans)
                {
                    sw.WriteLine($"{prefix}    if (_t_ == {real.Key}L)");
                    sw.WriteLine($"{prefix}        return new {real.Value.FullName}();");
                }
                sw.WriteLine($"{prefix}    if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)");
                sw.WriteLine($"{prefix}        return new Zeze.Transaction.EmptyBean();");
                sw.WriteLine($"{prefix}    return null;");
            }
            else
            {
                // 转发给全局静态（static）函数。
                sw.WriteLine($"{prefix}    return {type.DynamicParams.CreateBeanFromSpecialTypeId.Replace("::", ".")}(_t_);");
            }
            sw.WriteLine($"{prefix}}}");
            sw.WriteLine();
        }

        public void WriteDefine(StreamWriter sw, Project project)
        {
            if (bean.CustomTypeId)
                throw new Exception("custom TypeId is NOT allowed for java: " + bean.Name);
            sw.WriteLine("    public static final long TYPEID = " + bean.TypeId + "L;");
            sw.WriteLine();
            // declare enums
            foreach (Enum e in bean.Enums)
            {
                sw.WriteLine($"    public static final {TypeName.GetName(Type.Compile(e.Type))} " + e.Name + " = " + e.Value + ";" + e.Comment);
            }
            if (bean.Enums.Count > 0)
            {
                sw.WriteLine();
            }

            // declare variables
            bool addBlankLine = false;
            foreach (Variable v in bean.Variables)
            {
                Type vt = v.VariableType;
                string final = vt is TypeCollection
                               || vt is TypeMap
                               || vt is Bean
                               || vt is TypeDynamic
                               || vt is TypeGTable
                    // || vt is TypeVector2
                    // || vt is TypeVector2Int
                    // || vt is TypeVector3
                    // || vt is TypeVector3Int
                    // || vt is TypeVector4
                    // || vt is TypeQuaternion
                    ? "final "
                    : "";

                if (vt is Bean)
                    sw.WriteLine($"    private {final}Zeze.Transaction.Collections.CollOne<{TypeName.GetName(vt)}> {v.NamePrivate};{v.Comment}");
                else
                    sw.WriteLine($"    private {final}{TypeName.GetName(vt)} {v.NamePrivate};{v.Comment}");

                // ReadOnlyMap
                /*
                if (vt is TypeMap pmap)
                {
                    var key = TypeName.GetName(pmap.KeyType);
                    var value = pmap.ValueType.IsNormalBean
                        ? TypeName.GetName(pmap.ValueType) + "ReadOnly"
                        : TypeName.GetName(pmap.ValueType);
                    var readonlyTypeName = $"Zeze.Transaction.Collections.CollMapReadOnly<{key},{value},{TypeName.GetName(pmap.ValueType)}>";
                    sw.WriteLine($"        private {readonlyTypeName} {v.NamePrivate}ReadOnly;");
                }
                */
                addBlankLine = false;
                if (vt is TypeDynamic dy0)
                    GenDynamicSpecialMethod(sw, "    ", v, dy0, false);
                else if (vt is TypeMap map && map.ValueType is TypeDynamic dy1)
                    GenDynamicSpecialMethod(sw, "    ", v, dy1, true);
                else if (vt is TypeSortedMap smap && smap.ValueType is TypeDynamic dy3)
                    GenDynamicSpecialMethod(sw, "    ", v, dy3, true);
                else if (vt is TypeCollection coll && coll.ValueType is TypeDynamic dy2)
                    GenDynamicSpecialMethod(sw, "    ", v, dy2, true);
                else if (vt is TypeCollection or TypeMap or TypeSortedMap or TypeGTable)
                    GenCollectionMetaDefine(sw, "    ", v);
                else
                    addBlankLine = true;
            }
            if (addBlankLine)
                sw.WriteLine();

            Property.Make(bean, sw, "    ");
            Construct.Make(bean, sw, "    ");
            Reset.Make(bean, sw, "    ", false, bean.Base == "");
            Assign.Make(bean, sw, "    ", project, bean.Base == "");
            // Copy
            sw.WriteLine("    public " + bean.Name + " copyIfManaged() {");
            sw.WriteLine("        return isManaged() ? copy() : this;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public " + bean.Name + " copy() {");
            sw.WriteLine("        var _c_ = new " + bean.Name + "();");
            sw.WriteLine("        _c_.assign(this);");
            sw.WriteLine("        return _c_;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine($"    public static void swap({bean.Name} _a_, {bean.Name} _b_) {{");
            sw.WriteLine($"        var _s_ = _a_.copy();");
            sw.WriteLine("        _a_.assign(_b_);");
            sw.WriteLine("        _b_.assign(_s_);");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public long typeId() {");
            sw.WriteLine("        return TYPEID;");
            sw.WriteLine("    }");
            sw.WriteLine();
            // Log.Make(bean, sw, "    ");
            Tostring.Make(bean, sw, "    ", false);
            Encode.Make(bean, sw, "    ", bean.Base == "");
            Decode.Make(bean, sw, "    ", bean.Base == "");
            Equal.Make(bean, sw, "    ", false); // 对Java项目来说因Zeze.History需要,所以必须生成
            if (bean.GenEquals)
                HashCode.Make(bean, sw, "    ", false);
            sw.WriteLine();
            InitChildrenTableKey.Make(bean, sw, "    ");
            // InitChildrenTableKey.MakeReset(bean, sw, "    ");
            NegativeCheck.Make(bean, sw, "    ");
            FollowerApply.Make(bean, sw, "    ");
            DecodeResultSet.Make(bean, sw, "    ");
            EncodeSQLStatement.Make(bean, sw, "    ");
            GenVariables(bean, sw, "    ");
        }

        public void GenVariables(Bean bean, StreamWriter sw, string prefix)
        {
            if (bean.VariablesIdOrder.Count > 0)
            {
                sw.WriteLine();
                sw.WriteLine($"{prefix}@Override");
                sw.WriteLine($"{prefix}public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {{");
                sw.WriteLine($"{prefix}    var _v_ = super.variables();");
                foreach (var v in bean.VariablesIdOrder)
                {
                    string type = v.Type;
                    string key = v.Key;
                    string value = v.Value;

                    var vType = v.VariableType;
                    type = Variable.GetTypeFullName(vType);
                    if (vType.IsCollection)
                    {
                        if (vType is TypeMap map)
                        {
                            key = Variable.GetTypeFullName(map.KeyType);
                            value = Variable.GetTypeFullName(map.ValueType);
                        }
                        else if (vType is TypeSortedMap smap)
                        {
                            key = Variable.GetTypeFullName(smap.KeyType);
                            value = Variable.GetTypeFullName(smap.ValueType);
                        }
                        else if (vType is TypeList list)
                        {
                            value = Variable.GetTypeFullName(list.ValueType);
                        }
                        else if (vType is TypeSet set)
                        {
                            value = Variable.GetTypeFullName(set.ValueType);
                        }
                    }
                    else if (vType is TypeGTable table)
                    {
                        key = $"{Variable.GetTypeFullName(table.RowKeyType)},{Variable.GetTypeFullName(table.ColKeyType)}";
                        value = Variable.GetTypeFullName(table.ValueType);
                    }
                    sw.WriteLine($"{prefix}    _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data({v.Id}, \"{v.Name}\", \"{type}\", \"{key}\", \"{value}\"));");
                }
                sw.WriteLine($"{prefix}    return _v_;");
                sw.WriteLine($"{prefix}}}");
            }
        }
    }
}
