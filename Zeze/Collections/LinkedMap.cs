
using Zeze.Transaction;

namespace Zeze.Collections
{
    public abstract class LinkedMap : AbstractLinkedMap
    {
		internal static readonly BeanFactory BeanFactory = new BeanFactory();

		public static long GetSpecialTypeIdFromBean(Bean bean)
		{
			return bean.TypeId;
		}

		public static Bean CreateBeanFromSpecialTypeId(long typeId)
		{
			return BeanFactory.Create(typeId);
		}

	}
}
