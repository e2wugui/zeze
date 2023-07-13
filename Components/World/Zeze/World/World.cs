
using Zeze.Util;

namespace Zeze.World
{
    public class World : AbstractWorld
    {
        public static BeanFactory<Util.ConfBean> BeanFactory { get; } = new();

        public static long GetSpecialTypeIdFromBean(ConfBean bean)
        {
            return bean.TypeId;
        }

        public static ConfBean CreateBeanFromSpecialTypeId(long typeId)
        {
            return BeanFactory.Create(typeId);
        }

        protected override async System.Threading.Tasks.Task<long> ProcessCommand(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Builtin.World.Command;
            return Zeze.Util.ResultCode.NotImplement;
        }

    }
}
