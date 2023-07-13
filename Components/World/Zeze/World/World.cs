
namespace Zeze.World
{
    public class World : AbstractWorld
    {
        public static long GetSpecialTypeIdFromBean(Util.ConfBean bean)
        {
            return bean.TypeId;
        }

        public static Util.ConfBean CreateBeanFromSpecialTypeId(long typeId)
        {
            // TODO
            throw new NotImplementedException();
        }

        protected override async System.Threading.Tasks.Task<long> ProcessCommand(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Builtin.World.Command;
            return Zeze.Util.ResultCode.NotImplement;
        }

    }
}
