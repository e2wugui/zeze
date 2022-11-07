
namespace Zege
{
    public partial class UiApp : Microsoft.Maui.Controls.Application
    {
        public UiApp()
        {
            InitializeComponent();

            MainPage = new AppShell();
        }
    }
}