using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Transaction
{
	/// <summary>
	/// NOTE:<br>
	/// (1) 单参数OnChanged()回调，不仅仅是在事务中Add事件发生的时候进行回调，
	/// 如果在一个事务中，进行了删增操作，最后导致该记录被覆盖的情况也进行回调，
	/// <-- 这种情况下在xdb的框架内无法获得旧值，因此无法更改为三参数具有Note的OnChanged()回调 --> 
	/// 这也是为什么单参数OnChanged()不叫做OnAdded()的原因。;-) 
	/// (2) 三参数的OnChanged()是指记录没有增删，但是记录的内容变化了，变化的细节可以通过note得到
	/// </summary>
	public interface ChangeListener
	{
		public void OnChanged(TableKey tkey, Changes.Record changes);
	}

	/// <summary>
	/// 管理表格的数据变更订阅者。每张表拥有一个自己的listener管理对象。 功能：增加；删除；查询；触发回调
	/// </summary>
	public sealed class ChangeListenerMap
	{
		private HashSet<ChangeListener> Listnerers = new();
		internal volatile HashSet<ChangeListener> VolatileListnerers;

		public void AddListener(ChangeListener listener)
        {
			lock (this)
            {
				Listnerers.Add(listener);
				VolatileListnerers = null;
			}
		}

		public void RemoveListener(ChangeListener listener)
        {
			lock (this)
            {
				Listnerers.Remove(listener);
				VolatileListnerers = null;
			}
		}

		public IReadOnlySet<ChangeListener> GetListeners()
        {
			var tmp = VolatileListnerers;
			if (null != tmp)
				return tmp;

			lock (this)
			{
				tmp = VolatileListnerers;
				if (null == tmp)
				{
					tmp = new HashSet<ChangeListener>();
					foreach (var e in Listnerers)
						tmp.Add(e);
					VolatileListnerers = tmp;
				}
				return tmp;
			}
		}

		public bool HasListener => GetListeners().Count > 0;
	}
}
