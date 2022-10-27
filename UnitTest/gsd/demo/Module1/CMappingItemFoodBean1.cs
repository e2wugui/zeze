namespace demo.Module1
{
    public class CMappingItemFoodBean1  : CMappingItemFood
    {
        private demo.Bean1 _Bean;
        public CMappingItemFoodBean1(demo.Module1.Item base0, demo.Module1.Food base1, demo.Bean1 base2)
            : base( base0,  base1)
        {
            _Bean = base2;
        }
    }
}

