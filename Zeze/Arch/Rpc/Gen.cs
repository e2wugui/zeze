using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Arch.Rpc
{
    // 系列化代码生成
    public class Gen
    {
        private Dictionary<Type, (
            Action<StringBuilder, string, string>, // encoder
            Action<StringBuilder, string, string>, // decoder
            Action<StringBuilder, string, string>, // define local variable
            Func<string> // typename
            )>
            Serializer = new Dictionary<Type, (
                Action<StringBuilder, string, string>,
                Action<StringBuilder, string, string>,
                Action<StringBuilder, string, string>,
                Func<string>
                )>();

        private Dictionary<string, (
            Action<StringBuilder, string, string, Type, Type>, // encoder
            Action<StringBuilder, string, string, Type, Type>, // decoder
            Action<StringBuilder, string, string, Type, Type>, // define local variable
            Func<Type, Type, string> // typename
            )>
            System_Collections_Generic_Serializer = new Dictionary<string, (
                Action<StringBuilder, string, string, Type, Type>,
                Action<StringBuilder, string, string, Type, Type>,
                Action<StringBuilder, string, string, Type, Type>,
                Func<Type, Type, string>
                )>();

        public readonly static Gen Instance = new Gen();

        internal Zeze.Util.AtomicLong TmpVarNameId = new Zeze.Util.AtomicLong();

        private Gen()
        {
            /*
            Serializer[typeof(void)] = (
                (sb, prefix, varName) => { },
                (sb, prefix, varName) => { },
                (sb, prefix, varName) => { },
                () => "void"
                );
            */

            Serializer[typeof(Zeze.Net.Binary)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteBinary({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadBinary();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}Zeze.Net.Binary {varName} = null;"),
                () => "Zeze.Net.Binary"
                );

            Serializer[typeof(bool)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteBool({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadBool();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}bool {varName} = false;"),
                () => "bool"
                );

            Serializer[typeof(byte)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteByte({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadByte();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}byte {varName} = 0;"),
                () => "byte"
                );

            Serializer[typeof(Zeze.Serialize.ByteBuffer)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteByteBuffer({varName});"),
                // 这里不用ReadByteBuffer，这个方法和原来的buffer共享内存，除了编解码时用用，开放给应用不大好。
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = Zeze.Serialize.ByteBuffer.Wrap(_bb_.ReadBytes());"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}Zeze.Serialize.ByteBuffer {varName} = null;"),
                () => "Zeze.Serialize.ByteBuffer"
                );

            Serializer[typeof(byte[])] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteBytes({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadBytes();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}byte[] {varName} = null;"),
                () => "byte[]"
                );

            Serializer[typeof(double)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteDouble({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadDouble();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}double {varName} = 0.0;"),
                () => "double"
                );

            Serializer[typeof(float)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteFloat({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadFloat();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}float {varName} = 0.0;"),
                () => "float"
                );

            Serializer[typeof(int)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteInt({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadInt();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}int {varName} = 0;"),
                () => "int"
                );

            Serializer[typeof(long)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteLong({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadLong();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}long {varName} = 0;"),
                () => "long"
                );

            Serializer[typeof(short)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteShort({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadShort();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}short {varName} = 0;"),
                () => "short"
                );

            Serializer[typeof(string)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteString({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadString();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}string {varName} = null;"),
                () => "string"
               );

            Serializer[typeof(uint)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteUInt({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadUInt();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}uint {varName} = 0;"),
                () => "uint"
                );

            Serializer[typeof(ulong)] = (
                (sb, prefix, varName) => sb.AppendLine($"{prefix}_bb_.WriteUlong({varName});"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}{varName} = _bb_.ReadUlong();"),
                (sb, prefix, varName) => sb.AppendLine($"{prefix}ulong {varName} = 0;"),
                () => "ulong"
                );

            /////////////////////////////////////////////////////////////////////////
            ///
            System_Collections_Generic_Serializer["System.Collections.Generic.Dictionary"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.Dictionary<{GetTypeName(key)}, {GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.Dictionary<{GetTypeName(key)}, {GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.HashSet"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.HashSet<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.HashSet<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.ICollection"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.List<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.ICollection<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.IDictionary"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.Dictionary<{GetTypeName(key)}, {GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.IDictionary<{GetTypeName(key)}, {GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.IEnumerable"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.List<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.IEnumerable<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.IList"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.List<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.IList<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.IReadOnlyCollection"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.List<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.IReadOnlyCollection<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.IReadOnlyDictionary"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.Dictionary<{GetTypeName(key)}, {GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.IReadOnlyDictionary<{GetTypeName(key)}, {GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.IReadOnlyList"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.List<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.IReadOnlyList<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.IReadOnlySet"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.HashSet<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.IReadOnlySet<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.ISet"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.HashSet<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.ISet<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.KeyValuePair"] = (
                (sb, prefix, varName, key, value) =>
                {
                    GenEncode(sb, prefix, key, $"{varName}.Key");
                    GenEncode(sb, prefix, value, $"{varName}.Value");
                },
                (sb, prefix, varName, key, value) =>
                {
                    string tmpKey = "tmpKey" + TmpVarNameId.IncrementAndGet();
                    string tmpValue = "tmpValue" + TmpVarNameId.IncrementAndGet();
                    GenLocalVariable(sb, prefix, key, tmpKey);
                    GenLocalVariable(sb, prefix, value, tmpValue);
                    GenDecode(sb, prefix, key, $"{tmpKey}");
                    GenDecode(sb, prefix, value, $"{tmpValue}");
                    sb.AppendLine($"{prefix}{varName} = new System.Collections.Generic.KeyValuePair<{GetTypeName(key)}, {GetTypeName(value)}>({tmpKey}, {tmpValue});");
                },
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}System.Collections.Generic.KeyValuePair<{GetTypeName(key)}, {GetTypeName(value)}> {varName} = null;"),
                (key, value) => $"System.Collections.Generic.KeyValuePair<{GetTypeName(key)}, {GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.LinkedList"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.LinkedList<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.LinkedList<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.List"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.List<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.List<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.Queue"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.Queue<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.Queue<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.SortedDictionary"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.SortedDictionary<{GetTypeName(key)}, {GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.SortedDictionary<{GetTypeName(key)}, {GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.SortedList"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_2(sb, prefix, varName, key, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.SortedList<{GetTypeName(key)}, {GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.SortedList<{GetTypeName(key)}, {GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.SortedSet"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => GenDecodeGeneric_1(sb, prefix, varName, value),
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.SortedSet<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.SortedSet<{GetTypeName(value)}>"
            );

            System_Collections_Generic_Serializer["System.Collections.Generic.Stack"] = (
                (sb, prefix, varName, key, value) => GenEncodeGeneric_1(sb, prefix, varName, value), // 反过来的，see decode。
                (sb, prefix, varName, key, value) =>
                {
                    string tmpi = "tmp" + TmpVarNameId.IncrementAndGet();
                    string tmpValue = "tmpValue" + TmpVarNameId.IncrementAndGet();
                    string tmpReverse = "tmpReverse" + TmpVarNameId.IncrementAndGet();
                    string tmpReverseValue = "tmpReverse" + TmpVarNameId.IncrementAndGet();
                    sb.AppendLine($"{prefix}int {tmpi} = _bb_.ReadInt();");
                    sb.AppendLine($"{prefix}{GetTypeName(value)} [] {tmpReverse} = new {GetTypeName(value)}[{tmpi}];");
                    sb.AppendLine($"{prefix}for (; {tmpi} > 0; --{tmpi})");
                    sb.AppendLine($"{prefix}{{");
                    GenLocalVariable(sb, prefix + "    ", value, tmpValue);
                    GenDecode(sb, prefix + "    ", value, $"{tmpValue}");
                    sb.AppendLine($"{prefix}    {tmpReverse}[{tmpi} - 1] = {tmpValue};");
                    sb.AppendLine($"{prefix}}}");
                    sb.AppendLine($"{prefix}foreach (var {tmpReverseValue} in {tmpReverse})");
                    sb.AppendLine($"{prefix}{{");
                    sb.AppendLine($"{prefix}    {varName}.Push({tmpReverseValue});");
                    sb.AppendLine($"{prefix}}}");
                },
                (sb, prefix, varName, key, value) => sb.AppendLine($"{prefix}var {varName} = new System.Collections.Generic.Stack<{GetTypeName(value)}>();"),
                (key, value) => $"System.Collections.Generic.Stack<{GetTypeName(value)}>"
            );
        }

        private void GenEncodeGeneric_2(StringBuilder sb, string prefix, string varName, Type key, Type value)
        {
            string tmp = "tmp" + TmpVarNameId.IncrementAndGet();
            sb.AppendLine($"{prefix}_bb_.WriteInt({varName}.Count);");
            sb.AppendLine($"{prefix}foreach (var {tmp} in {varName})");
            sb.AppendLine($"{prefix}{{");
            GenEncode(sb, prefix + "    ", key, $"{tmp}.Key");
            GenEncode(sb, prefix + "    ", value, $"{tmp}.Value");
            sb.AppendLine($"{prefix}}}");
        }

        private void GenDecodeGeneric_2(StringBuilder sb, string prefix, string varName, Type key, Type value)
        {
            string tmpi = "tmp" + TmpVarNameId.IncrementAndGet();
            string tmpKey = "tmpKey" + TmpVarNameId.IncrementAndGet();
            string tmpValue = "tmpValue" + TmpVarNameId.IncrementAndGet();
            sb.AppendLine($"{prefix}for (int {tmpi} = _bb_.ReadInt(); {tmpi} > 0; --{tmpi})");
            sb.AppendLine($"{prefix}{{");
            GenLocalVariable(sb, prefix + "    ", key, tmpKey);
            GenLocalVariable(sb, prefix + "    ", value, tmpValue);
            GenDecode(sb, prefix + "    ", key, $"{tmpKey}");
            GenDecode(sb, prefix + "    ", value, $"{tmpValue}");
            sb.AppendLine($"{prefix}    {varName}.Add({tmpKey}, {tmpValue});");
            sb.AppendLine($"{prefix}}}");
        }

        private void GenEncodeGeneric_1(StringBuilder sb, string prefix, string varName, Type value)
        {
            string tmp = "tmp" + TmpVarNameId.IncrementAndGet();
            sb.AppendLine($"{prefix}_bb_.WriteInt({varName}.Count);");
            sb.AppendLine($"{prefix}foreach (var {tmp} in {varName})");
            sb.AppendLine($"{prefix}{{");
            GenEncode(sb, prefix + "    ", value, $"{tmp}");
            sb.AppendLine($"{prefix}}}");
        }

        private void GenDecodeGeneric_1(StringBuilder sb, string prefix, string varName, Type value)
        {
            string tmpi = "tmp" + TmpVarNameId.IncrementAndGet();
            string tmpValue = "tmpValue" + TmpVarNameId.IncrementAndGet();
            sb.AppendLine($"{prefix}for (int {tmpi} = _bb_.ReadInt(); {tmpi} > 0; --{tmpi})");
            sb.AppendLine($"{prefix}{{");
            GenLocalVariable(sb, prefix + "    ", value, tmpValue);
            GenDecode(sb, prefix + "    ", value, $"{tmpValue}");
            sb.AppendLine($"{prefix}    {varName}.Add({tmpValue});");
            sb.AppendLine($"{prefix}}}");
        }

        public string GetTypeName(Type type)
        {
            if (type.IsByRef)
                type = type.GetElementType();

            if (Serializer.TryGetValue(type, out var basic))
            {
                return basic.Item4();
            }

            if (typeof(Zeze.Serialize.Serializable).IsAssignableFrom(type))
            {
                return type.FullName;
            }

            string fullName = GetFullNameNoGenericParameters(type);
            if (false == type.IsGenericType)
                return fullName;

            Type[] parameters = type.GenericTypeArguments;
            if (System_Collections_Generic_Serializer.TryGetValue(fullName, out var generic))
            {
                switch (parameters.Length)
                {
                    case 1:
                        return generic.Item4(null, parameters[0]);

                    case 2:
                        return generic.Item4(parameters[0], parameters[1]);

                    default:
                        break; // fall down.
                }
            }
            fullName += "<";
            bool first = true;
            foreach (var parameter in parameters)
            {
                if (first)
                    first = false;
                else
                    fullName += ", ";
                fullName += GetTypeName(parameter);
            }
            fullName += ">";
            return fullName;
        }

        public void GenLocalVariable(StringBuilder sb, string prefix, Type type, string varName)
        {
            if (type.IsByRef)
                type = type.GetElementType();

            if (Serializer.TryGetValue(type, out var basic))
            {
                basic.Item3(sb, prefix, varName);
                return;
            }

            if (typeof(Zeze.Serialize.Serializable).IsAssignableFrom(type))
            {
                sb.AppendLine($"{prefix}{type.FullName} {varName} = new {type.FullName}();");
                return;
            }

            string typename = GetTypeName(type);
            if (false == type.IsGenericType)
            {
                // decode 不需要初始化。JsonSerializer.Deserialize
                sb.AppendLine($"{prefix}{typename} {varName} = default({typename});");
                return;
            }
            Type[] parameters = type.GenericTypeArguments;
            if (System_Collections_Generic_Serializer.TryGetValue(GetFullNameNoGenericParameters(type), out var generic))
            {
                switch (parameters.Length)
                {
                    case 1:
                        generic.Item3(sb, prefix, varName, null, parameters[0]);
                        return;

                    case 2:
                        generic.Item3(sb, prefix, varName, parameters[0], parameters[1]);
                        return;

                    default:
                        break; // fall down.
                }
            }
            // decode 不需要初始化。JsonSerializer.Deserialize
            sb.AppendLine($"{prefix}{typename} {varName} = default({typename});");
        }

        public void GenEncode(StringBuilder sb, string prefix, Type type, string varName)
        {
            if (type.IsByRef)
                type = type.GetElementType();

            if (Serializer.TryGetValue(type, out var basic))
            {
                basic.Item1(sb, prefix, varName);
                return;
            }

            if (typeof(Zeze.Serialize.Serializable).IsAssignableFrom(type))
            {
                sb.AppendLine($"{prefix}{varName}.Encode(_bb_);");
                return;
            }

            if (type.IsGenericType)
            {
                if (System_Collections_Generic_Serializer.TryGetValue(GetFullNameNoGenericParameters(type), out var generic))
                {
                    Type[] parameters = type.GenericTypeArguments;
                    switch (parameters.Length)
                    {
                        case 1:
                            generic.Item1(sb, prefix, varName, null, parameters[0]);
                            return;

                        case 2:
                            generic.Item1(sb, prefix, varName, parameters[0], parameters[1]);
                            return;

                        default:
                            break; // fall down.
                    }
                }
                // fall down
            }

            // Utf8Json https://aloiskraus.wordpress.com/2019/09/29/net-serialization-benchmark-2019-roundup/
            sb.AppendLine($"{prefix}_bb_.WriteBytes(System.Text.Json.JsonSerializer.SerializeToUtf8Bytes({varName}, typeof({GetTypeName(type)})));");
        }

        public void GenDecode(StringBuilder sb, string prefix, Type type, string varName)
        {
            if (type.IsByRef)
                type = type.GetElementType();

            if (Serializer.TryGetValue(type, out var p))
            {
                p.Item2(sb, prefix, varName);
                return;
            }

            if (typeof(Zeze.Serialize.Serializable).IsAssignableFrom(type))
            {
                sb.AppendLine($"{prefix}{varName}.Decode(_bb_);");
                return;
            }

            if (type.IsGenericType)
            {
                if (System_Collections_Generic_Serializer.TryGetValue(GetFullNameNoGenericParameters(type), out var generic))
                {
                    Type[] parameters = type.GenericTypeArguments;
                    switch (parameters.Length)
                    {
                        case 1:
                            generic.Item2(sb, prefix, varName, null, parameters[0]);
                            return;

                        case 2:
                            generic.Item2(sb, prefix, varName, parameters[0], parameters[1]);
                            return;

                        default:
                            break; // fall down.
                    }
                }
                // fall down
            }

            string tmp1 = "tmp" + TmpVarNameId.IncrementAndGet();
            string tmp2 = "tmp" + TmpVarNameId.IncrementAndGet();
            sb.AppendLine($"{prefix}var {tmp1} = _bb_.ReadByteBuffer();");
            sb.AppendLine($"{prefix}var {tmp2} = new System.ReadOnlySpan<byte>({tmp1}.Bytes, {tmp1}.ReadIndex, {tmp1}.Size);");
            sb.AppendLine($"{prefix}{varName} = System.Text.Json.JsonSerializer.Deserialize<{GetTypeName(type)}> ({tmp2});");
        }

        public string GetFullNameNoGenericParameters(Type type)
        {
            string className = type.IsGenericType ? type.Name.Substring(0, type.Name.IndexOf('`')) : type.Name;
            // 处理嵌套类名字。
            string fullName = className;
            for (Type declaring = type.DeclaringType; declaring != null; declaring = declaring.DeclaringType)
            {
                fullName = declaring.Name + "." + fullName;
            }
            return null != type.Namespace ? type.Namespace + "." + fullName : fullName;
        }

        public void GenEncode(StringBuilder sb, string prefix, List<ParameterInfo> parameters)
        {
            for (int i = 0; i < parameters.Count; ++i)
            {
                var p = parameters[i];
                if (p.IsOut)
                    continue;
                if (IsDelegate(p.ParameterType))
                    continue;
                GenEncode(sb, prefix, p.ParameterType, p.Name);
            }
        }

        public void GenDecode(StringBuilder sb, string prefix, List<ParameterInfo> parameters)
        {
            for (int i = 0; i < parameters.Count; ++i)
            {
                var p = parameters[i];
                if (p.IsOut)
                    continue;
                if (IsDelegate(p.ParameterType))
                    continue;
                GenDecode(sb, prefix, p.ParameterType, p.Name);
            }
        }

        public static bool IsOnHashEnd(ParameterInfo pInfo)
        {
            var pType = pInfo.ParameterType;
            if (!IsActionDelegate(pType))
                return false;
            return IsOnHashEnd(pType.GetGenericArguments());
        }

        public static bool IsOnHashEnd(Type[] GenericArguments)
        {
            if (GenericArguments.Length != 1)
                return false;
            /*if (GenericArguments[0] != typeof(Zezex.Provider.ModuleProvider.ModuleRedirectAllContext))
                return false;
            TODO*/return true;
        }
        public static bool IsDelegate(Type type)
        {
            if (type.IsByRef)
                type = type.GetElementType();
            return type == typeof(Delegate) || type.IsSubclassOf(typeof(Delegate));
        }

        public static bool IsActionDelegate(Type sourceType)
        {
            if (sourceType.IsSubclassOf(typeof(MulticastDelegate)) &&
               sourceType.GetMethod("Invoke").ReturnType == typeof(void))
                return true;
            return false;
        }

        public string ToDefineString(ParameterInfo[] parameters)
        {
            StringBuilder sb = new StringBuilder();
            bool first = true;
            foreach (var p in parameters)
            {
                if (first)
                    first = false;
                else
                    sb.Append(", ");
                string prefix = "";
                if (p.IsOut)
                    prefix = "out ";
                else if (p.ParameterType.IsByRef)
                    prefix = "ref ";
                sb.Append(prefix).Append(GetTypeName(p.ParameterType)).Append(" ").Append(p.Name);
            }
            return sb.ToString();
        }
    }
}
