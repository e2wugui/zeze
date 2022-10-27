namespace demo.Module1
{
    public class CMappingItemFood  : CMappingItem
    {
        private demo.Module1.Food _Bean;
        public CMappingItemFood(demo.Module1.Item base0, demo.Module1.Food base1)
            : base( base0)
        {
            _Bean = base1;
        }
    }
}

