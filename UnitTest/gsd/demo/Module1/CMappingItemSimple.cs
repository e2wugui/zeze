namespace demo.Module1
{
    public class CMappingItemSimple  : CMappingItem
    {
        private demo.Module1.Simple _Bean;
        public CMappingItemSimple(demo.Module1.Item base0, demo.Module1.Simple base1)
            : base( base0)
        {
            _Bean = base1;
        }
    }
}

