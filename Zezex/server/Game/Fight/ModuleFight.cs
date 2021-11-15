
using System.Threading.Tasks;
using Zeze.Transaction;

namespace Game.Fight
{
    public sealed partial class ModuleFight : AbstractModule
    {
        public void Start(Game.App app)
        {
        }

        public void Stop(Game.App app)
        {
        }

        public Fighter GetFighter(BFighterId fighterId)
        {
            return new Fighter(fighterId, _tfighters.GetOrAdd(fighterId));
        }

        public long CalculateFighter(BFighterId fighterId)
        {
            // fighter 计算属性现在不主动通知客户端，需要客户端需要的时候来读取。

            Fighter fighter = new Fighter(fighterId, new BFighter());
            switch (fighterId.Type)
            {
                case BFighterId.TypeRole:
                    Game.App.Instance.Game_Buf.GetBufs(fighterId.InstanceId).CalculateFighter(fighter);
                    Game.App.Instance.Game_Equip.CalculateFighter(fighter);
                    break;
            }
            _tfighters.GetOrAdd(fighterId).Assign(fighter.Bean);
            return Procedure.Success;
        }

        public void StartCalculateFighter(long roleId)
        {
            BFighterId fighterId = new BFighterId(BFighterId.TypeRole, roleId);
            Task.Run(Game.App.Instance.Zeze.NewProcedure(() => CalculateFighter(fighterId), "CalculateFighter").Call);
        }
    }
}
