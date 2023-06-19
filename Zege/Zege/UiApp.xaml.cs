
namespace Zege
{
    public partial class UiApp : Microsoft.Maui.Controls.Application
    {
        public UiApp()
        {
            InitializeComponent();

            MainPage = new LoginShell();
        }

        protected override Microsoft.Maui.Controls.Window CreateWindow(IActivationState activationState)
        {
            var mainWindow = base.CreateWindow(activationState);
            mainWindow.Destroying += MainWindow_Destroying;
            return mainWindow;
        }

        private void MainWindow_Destroying(object sender, EventArgs e)
        {
            foreach (var win in Windows)
            {
                if (win != sender)
                    CloseWindow(win);
            }
        }
    }
}