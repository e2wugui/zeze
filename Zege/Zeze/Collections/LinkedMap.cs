using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zege.Friend;
using Zeze.Util;

namespace Zeze.Collections
{
    public class LinkedMap
    {
        public static long GetSpecialTypeIdFromBean(ConfBean bean)
        {
            return bean.TypeId;
        }

        public static ConfBean CreateBeanFromSpecialTypeId(long typeId)
        {
            switch (typeId)
            {
                case BFriend.TYPEID:
                    return new BFriend();

                default:
                    throw new Exception("Unknown Dynamic TypeId");
            }
        }
    }
}
