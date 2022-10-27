namespace demo.Module1
{
    public class CMappingItemBean1  : CMappingItem
    {
        private demo.Bean1 _Bean;
        public CMappingItemBean1(demo.Module1.Item base0, demo.Bean1 base1)
            : base( base0)
        {
            _Bean = base1;
        }
    }
}

