
namespace Game.Login
{
    public sealed partial class Module : AbstractModule
    {
        public void Start(Game.App app)
        {
        }

        public void Stop(Game.App app)
        {
        }

        public override int ProcessSAuth(SAuth protocol)
        {
            return Zeze.Transaction.Procedure.NotImplement;
        }

        public override int ProcessSCreateRole(SCreateRole protocol)
        {
            return Zeze.Transaction.Procedure.NotImplement;
        }

        public override int ProcessSEnterWorldNow(SEnterWorldNow protocol)
        {
            return Zeze.Transaction.Procedure.NotImplement;
        }

        public override int ProcessSGetRoleList(SGetRoleList protocol)
        {
            return Zeze.Transaction.Procedure.NotImplement;
        }

    }
}
