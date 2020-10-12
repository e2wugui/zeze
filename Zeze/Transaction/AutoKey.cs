using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;

namespace Zeze.Transaction
{
    public sealed class AutoKeys
    {
		public int LocalId { get; }
		public int LocalStep { get; }

		private Dictionary<AutoKey, AutoKey> map = new Dictionary<AutoKey, AutoKey>();
		internal Util.AtomicBool Dirty { get; } = new Util.AtomicBool();

		/// <summary>
		/// 这个不考虑效率，使用的时候应该保存返回的AutoKey以便再次使用。
		/// </summary>
		/// <param name="name"></param>
		/// <returns></returns>
		public AutoKey GetAutoKey(string name)
        {
			lock (this)
            {
				AutoKey tmp = new AutoKey(this, name, LocalId, LocalStep);
				if (map.TryGetValue(tmp, out var exist))
				{
					return exist;
				}

				map.Add(tmp, tmp);
				Dirty.GetAndSet(true);
				return tmp;
			}
		}

		internal ByteBuffer Encode()
        {
			if (Dirty.CompareAndExchange(true, false))
            {
				ByteBuffer os = ByteBuffer.Allocate();
				os.WriteInt(map.Count);
				foreach (var key in map.Keys)
                {
					key.Encode(os);
                }
				return os;
            }
			return null;
        }

		internal AutoKeys(ByteBuffer os, int localInitValue, int localStep)
        {
			if (localStep <= 0 || localInitValue < 0 || localInitValue >= localStep)
			{
				throw new Exception("AutoKeys Invalid localInitValue or localStep");
			}

			LocalId = localInitValue;
			LocalStep = localStep;

			if (null != os)
			{
				for (int n = os.ReadInt(); n > 0; --n)
                {
					AutoKey autoKey = new AutoKey(this, os);
					map.Add(autoKey, autoKey);
                }
			}
        }
	}

	public sealed class AutoKey
	{
		public AutoKeys AutoKeys { get; }
		public string Name { get; }
		public int LocalId { get; }
		public int LocalStep { get; }
		public long Current { get; private set; }

		public void Accept(long key)
        {
			if ((key % LocalStep) != LocalId || key <= 0)
				return; // 忽略不是本地 LocalInitValue 的 key。不再报错。倒数据时可能需要倒入其他LocalInitValue的key。

			lock (this)
            {
				if (key > Current)
                {
					Current = key;
					AutoKeys.Dirty.GetAndSet(true);
				}
			}
		}

		public long Next()
        {
			if (Transaction.Current == null)
				throw new Exception("Not in transaction");

			lock (this)
            {
				long tmp = Current + LocalStep;
				if (tmp < 0)
					throw new Exception("autokey expired! " + Name);
				Current = tmp;
				AutoKeys.Dirty.GetAndSet(true);
				return tmp;
            }
        }

        public override int GetHashCode()
        {
            return LocalId ^ Name.GetHashCode();
        }

        public override bool Equals(object obj)
        {
			if (obj is AutoKey another)
            {
				return LocalId == another.LocalId && Name.Equals(another.Name);
            }
			return false;
        }

        public override string ToString()
        {
			return $"({Name},{LocalId},{LocalStep},{Current}";
        }

        internal AutoKey(AutoKeys autoKeys, ByteBuffer os)
        {
			this.AutoKeys = autoKeys;

			Name = os.ReadString();
			LocalId = os.ReadInt();
			LocalStep = os.ReadInt();
			Current = os.ReadLong();
        }

		internal AutoKey(AutoKeys autoKeys, string name, int localInitValue, int localStep)
        {
			this.AutoKeys = autoKeys;

			this.Name = name;
			this.LocalId = localInitValue;
			this.LocalStep = localStep;
			this.Current = localInitValue;
        }

		internal void Encode(ByteBuffer os)
        {
			os.WriteString(Name);
			os.WriteInt(LocalId);
			os.WriteInt(LocalStep);
			lock (this) // 在Transaction.FlushReadWriteLock保护下应该是不需要需要的。
            {
				os.WriteLong(Current);
            }
        }
	}
}
