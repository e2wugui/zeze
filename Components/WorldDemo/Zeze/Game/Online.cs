using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Util;

namespace Zeze.Game
{
    public class Online
    {
        public static BeanFactory<ConfBean> BeanFactory { get; } = new BeanFactory<ConfBean>();

        public static long GetSpecialTypeIdFromBean(ConfBean bean)
        {
            return bean.TypeId;
        }

        public static ConfBean CreateBeanFromSpecialTypeId(long typeId)
        {
            return BeanFactory.Create(typeId);
        }

    }
}
