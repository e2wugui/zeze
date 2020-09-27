
namespace Game.Bag
{
    public sealed partial class Module : AbstractModule
    {
        public void Start(Game.App app)
        {
        }

        public void Stop(Game.App app)
        {
        }

        public override int ProcessCMove(CMove protocol)
        {
            throw new System.NotImplementedException();
        }

        public override int ProcessCDestroy(CDestroy protocol)
        {
            throw new System.NotImplementedException();
        }

        public override int ProcessCSort(CSort protocol)
        {
            throw new System.NotImplementedException();
        }
    }
}
