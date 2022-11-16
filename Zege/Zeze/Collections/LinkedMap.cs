using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
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
                case Zege.Friend.BFriend.TYPEID:
                    return new Zege.Friend.BFriend();

                case Zege.Notify.BNotify.TYPEID:
                    return new Zege.Notify.BNotify();

                default:
                    throw new Exception("Unknown Dynamic TypeId");
            }
        }
    }
}
