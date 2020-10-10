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
		/// <summary>
		/// 新增记录 或 覆盖记录时，进行回调。需要同步所有数据。
		/// </summary>
		/// <param name="key"></param>
		/// <param name="value"></param>
		public void OnChanged(object key, Bean value);

		/// <summary>
		/// 删除记录时，进行回调。
		/// </summary>
		/// <param name="key"></param>
		public void OnRemoved(object key);

		/// <summary>
		/// 记录脏了的时候进行回调，监听Map，Set类型的记录项具会有Note信息
		/// Note信息可以用来做增量数据同步。
		/// </summary>
		/// <param name="key"></param>
		/// <param name="note"></param>
		public void OnChanged(object key, ChangeNote note);
	}
}
