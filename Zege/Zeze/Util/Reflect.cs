using System;
using System.Collections.Generic;
using System.Reflection;
using Zeze.Transaction;
using Zeze.Transaction.Collections;

namespace Zeze.Util
{
    public class Reflect
    {
        public Dictionary<string, MethodInfo> Methods { get; } = new Dictionary<string, MethodInfo>();

        public static Type GetType(string className)
        {
            foreach (var assembly in AppDomain.CurrentDomain.GetAssemblies())
            {
                try
                {
                    var type = assembly.GetType(className);
                    if (type != null)
                        return type;
                }
                catch (Exception)
                {
                    // ignored
                }
            }
            throw new Exception($"{className} Not Found.");
        }

        public Reflect(IReflect type)
        {
            foreach (var m in type.GetMethods(BindingFlags.Instance | BindingFlags.NonPublic | BindingFlags.Public))
            {
                if (m.Name.StartsWith("Process")) // 只有协议处理函数能配置TransactionLevel
                    Methods.Add(m.Name, m);
            }
        }

        // ReSharper disable UnusedParameter.Global
        public TransactionLevel GetTransactionLevel(string methodName, TransactionLevel def)
        {
#if USE_CONFCS
            return TransactionLevel.None;
#else
            if (Methods.TryGetValue(methodName, out var method))
            {
                var attr = method.GetCustomAttribute<TransactionLevelAttribute>();
                if (attr != null)
                    return attr.Level;
                // else def
            }
            return def;
#endif
        }
        // ReSharper restore UnusedParameter.Global

        public DispatchMode GetDispatchMode(string methodName, DispatchMode def)
        {
            if (Methods.TryGetValue(methodName, out var method))
            {
                var annotation = method.GetCustomAttribute<DispatchModeAttribute>();
                if (annotation != null)
                    return annotation.Mode;
            }
            return def;
        }

        // 不想提取公共函数了。下面两个版本实现拷贝出来修改。
#if USE_CONFCS
        public static string GetStableName(Type type)
        {
            if (type.IsGenericType)
            {
                var def = type.GetGenericTypeDefinition();

                // Zeze.Transaction.Logs
                if (def == typeof(Log<>))
                    return $"Zeze.Transaction.Log<{GetStableName(type.GenericTypeArguments[0])}>";
                if (def == typeof(LogMap1<,>))
                    return $"Zeze.Transaction.Collections.LogMap1<{GetStableName(type.GenericTypeArguments[0])}, {GetStableName(type.GenericTypeArguments[1])}>";
                if (def == typeof(LogMap2<,>))
                    return $"Zeze.Transaction.Collections.LogMap2<{GetStableName(type.GenericTypeArguments[0])}, {GetStableName(type.GenericTypeArguments[1])}>";
                if (def == typeof(LogSet1<>))
                    return $"Zeze.Transaction.Collections.LogSet1<{GetStableName(type.GenericTypeArguments[0])}>";
                if (def == typeof(LogList1<>))
                    return $"Zeze.Transaction.Collections.LogList1<{GetStableName(type.GenericTypeArguments[0])}>";
                if (def == typeof(LogList2<>))
                    return $"Zeze.Transaction.Collections.LogList2<{GetStableName(type.GenericTypeArguments[0])}>";
                if (def == typeof(LogOne<>))
                    return $"Zeze.Transaction.Collections.LogOne<{GetStableName(type.GenericTypeArguments[0])}>";

                throw new Exception($"Unsupported Generic Type {type.FullName}");
            }

            // 支持的 Zeze/Gen/Types/ 类型。
            // 必须是xml中定义Bean.Variable.Type的名字。
            if (type == typeof(bool))
                return "bool";
            if (type == typeof(byte))
                return "byte";
            if (type == typeof(short))
                return "short";
            if (type == typeof(int))
                return "int";
            if (type == typeof(long))
                return "long";

            if (type == typeof(float))
                return "float";
            if (type == typeof(double))
                return "double";

            if (type == typeof(Net.Binary))
                return "binary";
            if (type == typeof(string))
                return "string";

            if (type == typeof(LogConfDynamic))
                return "Zeze.Transaction.LogDynamic";

            if (typeof(Serialize.Serializable).IsAssignableFrom(type))
                return type.FullName;

            // Serializable已经处理了下面这两种情况，不会执行到这里，写在这里，明确一下类型。
            /*
            if (type.IsAssignableTo(typeof(Zeze.Transaction.Bean)))
                return type.FullName;
            if (type.IsAssignableTo(typeof(Zeze.Raft.RocksRaft.Bean)))
                return type.FullName;
            */

            throw new Exception($"Unsupported type {type.FullName}");
        }
#else
        public static string GetStableName(Type type)
		{
			if (type.IsGenericType)
			{
				var def = type.GetGenericTypeDefinition();

				// Zeze.Transaction.Logs
				if (def == typeof(Zeze.Transaction.Log<>))
					return $"Zeze.Transaction.Log<{GetStableName(type.GenericTypeArguments[0])}>";
				if (def == typeof(Zeze.Transaction.Collections.LogMap1<,>))
					return $"Zeze.Transaction.Collections.LogMap1<{GetStableName(type.GenericTypeArguments[0])}, {GetStableName(type.GenericTypeArguments[1])}>";
				if (def == typeof(Zeze.Transaction.Collections.LogMap2<,>))
					return $"Zeze.Transaction.Collections.LogMap2<{GetStableName(type.GenericTypeArguments[0])}, {GetStableName(type.GenericTypeArguments[1])}>";
				if (def == typeof(Zeze.Transaction.Collections.LogSet1<>))
					return $"Zeze.Transaction.Collections.LogSet1<{GetStableName(type.GenericTypeArguments[0])}>";
				if (def == typeof(Zeze.Transaction.Collections.LogList1<>))
					return $"Zeze.Transaction.Collections.LogList1<{GetStableName(type.GenericTypeArguments[0])}>";
				if (def == typeof(Zeze.Transaction.Collections.LogList2<>))
					return $"Zeze.Transaction.Collections.LogList2<{GetStableName(type.GenericTypeArguments[0])}>";

                if (def == typeof(Zeze.Transaction.Collections.LogOne<>))
                    return $"Zeze.Transaction.Collections.LogOne<{GetStableName(type.GenericTypeArguments[0])}>";

                // Zeze.Raft.RocksRaft.Logs
                if (def == typeof(Zeze.Raft.RocksRaft.Log<>))
					return $"Zeze.Raft.RocksRaft.Log<{GetStableName(type.GenericTypeArguments[0])}>";
				if (def == typeof(Zeze.Raft.RocksRaft.LogMap1<,>))
					return $"Zeze.Raft.RocksRaft.LogMap1<{GetStableName(type.GenericTypeArguments[0])}, {GetStableName(type.GenericTypeArguments[1])}>";
				if (def == typeof(Zeze.Raft.RocksRaft.LogMap2<,>))
					return $"Zeze.Raft.RocksRaft.LogMap2<{GetStableName(type.GenericTypeArguments[0])}, {GetStableName(type.GenericTypeArguments[1])}>";
				if (def == typeof(Zeze.Raft.RocksRaft.LogSet1<>))
					return $"Zeze.Raft.RocksRaft.LogSet1<{GetStableName(type.GenericTypeArguments[0])}>";
				if (def == typeof(Zeze.Raft.RocksRaft.LogList1<>))
					return $"Zeze.Raft.RocksRaft.LogList1<{GetStableName(type.GenericTypeArguments[0])}>";
				if (def == typeof(Zeze.Raft.RocksRaft.LogList2<>))
					return $"Zeze.Raft.RocksRaft.LogList2<{GetStableName(type.GenericTypeArguments[0])}>";

				// 下面的反射目前实际上不需要，先写在这里。
				/*
                // Zeze.Transaction.Collections
                if (def == typeof(Zeze.Transaction.Collections.PList1<>))
                    return $"Zeze.Transaction.Collections.PList1<{GetStableName(type.GenericTypeArguments[0])}>";
                if (def == typeof(Zeze.Transaction.Collections.PList2<>))
                    return $"Zeze.Transaction.Collections.PList2<{GetStableName(type.GenericTypeArguments[0])}>";
                if (def == typeof(Zeze.Transaction.Collections.PSet1<>))
                    return $"Zeze.Transaction.Collections.PSet1<{GetStableName(type.GenericTypeArguments[0])}>";
                if (def == typeof(Zeze.Transaction.Collections.PMap1<,>))
                    return $"Zeze.Transaction.Collections.PMap1<{GetStableName(type.GenericTypeArguments[0])}, {GetStableName(type.GenericTypeArguments[1])}>";
                if (def == typeof(Zeze.Transaction.Collections.PMap2<,>))
                    return $"Zeze.Transaction.Collections.PMap2<{GetStableName(type.GenericTypeArguments[0])}, {GetStableName(type.GenericTypeArguments[1])}>";
                
                // Zeze.Raft.RocksRaft.Collections
                if (def == typeof(Zeze.Raft.RocksRaft.CollSet1<>))
                    return $"Zeze.Raft.RocksRaft.CollSet1<{GetStableName(type.GenericTypeArguments[0])}>";
                if (def == typeof(Zeze.Raft.RocksRaft.CollMap1<,>))
                    return $"Zeze.Raft.RocksRaft.CollMap1<{GetStableName(type.GenericTypeArguments[0])}, {GetStableName(type.GenericTypeArguments[1])}>";
                if (def == typeof(Zeze.Raft.RocksRaft.CollMap2<,>))
                    return $"Zeze.Raft.RocksRaft.CollMap2<{GetStableName(type.GenericTypeArguments[0])}, {GetStableName(type.GenericTypeArguments[1])}>";
                // */
				throw new Exception($"Unsupported Generic Type {type.FullName}");
			}

			// 支持的 Zeze/Gen/Types/ 类型。
			// 必须是xml中定义Bean.Variable.Type的名字。
			if (type == typeof(bool))
				return "bool";
			if (type == typeof(byte))
				return "byte";
			if (type == typeof(short))
				return "short";
			if (type == typeof(int))
				return "int";
			if (type == typeof(long))
				return "long";

			if (type == typeof(float))
				return "float";
			if (type == typeof(double))
				return "double";

            if (type == typeof(decimal))
                return "decimal";

            if (type == typeof(Zeze.Net.Binary))
				return "binary";
			if (type == typeof(string))
				return "string";

			if (type == typeof(Zeze.Util.LogConfDynamic))
				return "Zeze.Transaction.LogDynamic";

			if (type.IsAssignableTo(typeof(Zeze.Serialize.Serializable)))
				return type.FullName;

			// Serializable已经处理了下面这两种情况，不会执行到这里，写在这里，明确一下类型。
			/*
            if (type.IsAssignableTo(typeof(Zeze.Transaction.Bean)))
                return type.FullName;
            if (type.IsAssignableTo(typeof(Zeze.Raft.RocksRaft.Bean)))
                return type.FullName;
            */
			throw new Exception($"Unsupported type {type.FullName}");
		}
#endif
    }
}
