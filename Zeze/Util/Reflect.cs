using System;
using System.Collections.Generic;
using System.Reflection;
using Zeze.Transaction;

namespace Zeze.Util
{
    public class Reflect
    {
        public Dictionary<string, MethodInfo> Methods { get; } = new Dictionary<string, MethodInfo>();

		public static Type GetType(string className)
        {
			foreach (var assembly in AppDomain.CurrentDomain.GetAssemblies())
			{
				Type type = null;
				try
				{
					type = assembly.GetType(className);
				}
				catch (Exception)
				{
					continue;
				}
				if (null == type)
					continue;
				return type;
			}
			throw new Exception($"{className} Not Found.");
		}

		public Reflect(Type type)
        {
            foreach (var method in type.GetMethods(BindingFlags.Instance | BindingFlags.NonPublic | BindingFlags.Public))
            {
                if (method.Name.StartsWith("Process")) // 只有协议处理函数能配置TransactionLevel
                    Methods.Add(method.Name, method);
            }
        }

        public TransactionLevel GetTransactionLevel(string methodName, TransactionLevel def)
        {
            if (Methods.TryGetValue(methodName, out var method))
            {
                var attr = method.GetCustomAttribute<TransactionLevelAttribute>();
                if (attr != null)
                    return (TransactionLevel)TransactionLevel.Parse(typeof(TransactionLevel), attr.Level);
                // else def
            }
            return def;
        }


		public static string GetStableName(Type type)
		{
			if (type.IsGenericType)
			{
				var def = type.GetGenericTypeDefinition();

				// Zeze.Raft.RocksRaft.Logs
				if (def == typeof(Zeze.Raft.RocksRaft.Log<>))
					return $"Zeze.Raft.RocksRaft.Log<{GetStableName(type.GenericTypeArguments[0])}>";
				if (def == typeof(Zeze.Raft.RocksRaft.LogMap1<,>))
					return $"Zeze.Raft.RocksRaft.LogMap1<{GetStableName(type.GenericTypeArguments[0])}, {GetStableName(type.GenericTypeArguments[1])}>";
				if (def == typeof(Zeze.Raft.RocksRaft.LogMap2<,>))
					return $"Zeze.Raft.RocksRaft.LogMap2<{GetStableName(type.GenericTypeArguments[0])}, {GetStableName(type.GenericTypeArguments[1])}>";
				if (def == typeof(Zeze.Raft.RocksRaft.LogSet1<>))
					return $"Zeze.Raft.RocksRaft.LogSet1<{GetStableName(type.GenericTypeArguments[0])}>";

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
			// 必须是xml中定义Bean.Varialble.Type的名字。
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

			if (type == typeof(Zeze.Net.Binary))
				return "binary";
			if (type == typeof(string))
				return "string";

			if (type.IsAssignableTo(typeof(Zeze.Serialize.Serializable)))
				return type.FullName;

			// Serializable已经处理了下面这两种情况，不会执行到这里，写在这里，明确一下类型。
			if (type.IsAssignableTo(typeof(Zeze.Transaction.Bean)))
				return type.FullName;
			if (type.IsAssignableTo(typeof(Zeze.Raft.RocksRaft.Bean)))
				return type.FullName;

			throw new Exception($"Unsupported type {type.FullName}");
		}
	}
}
