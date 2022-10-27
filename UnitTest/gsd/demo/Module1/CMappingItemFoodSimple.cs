namespace demo.Module1
{
    public class CMappingItemFoodSimple  : CMappingItemFood
    {
        private demo.Module1.Simple _Bean;
        public CMappingItemFoodSimple(demo.Module1.Item base0, demo.Module1.Food base1, demo.Module1.Simple base2)
            : base( base0,  base1)
        {
            _Bean = base2;
        }
    }
}

