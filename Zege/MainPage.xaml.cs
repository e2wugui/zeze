using System.Collections.ObjectModel;

namespace Zege
{
    public class Friendx
    {
        public string Image { get; set; }
        public string Nick { get; set; }
        public string Time { get; set; }
        public string Message { get; set; }
    }

    public partial class MainPage : ContentPage
    {
        int count = 0;

        public ObservableCollection<Friendx> Friends { get; set; } = new();

        public MainPage()
        {
            InitializeComponent();
            for (int i = 0; i < 10; ++i)
            {
                var x = new Friendx()
                {
                    Image = "https://www.google.com/images/hpp/Chrome_Owned_96x96.png",
                    Nick = "Nick " + i,
                    Time = "12:00",
                    Message = "Hello World! Hello World! Hello World! Hello World! Hello World!"
                };
                Friends.Add(x);
            }
            FriendsListView.ItemsSource = Friends;
        }

        private void OnCounterClicked(object sender, EventArgs e)
        {
            count++;

            if (count == 1)
                CounterBtn.Text = $"Clicked {count} time";
            else
                CounterBtn.Text = $"Clicked {count} times";

            SemanticScreenReader.Announce(CounterBtn.Text);
        }

        private void OnAddHeadClicked(object sender, EventArgs e)
        {
            for (int i = 0; i < 3; ++i)
            {
                var x = new Friendx()
                {
                    Image = "https://www.google.com/images/hpp/Chrome_Owned_96x96.png",
                    // TODO BUG？结果是 2，2，1。
                    Nick = "AddHead " + i,
                    Time = "12:00",
                    Message = "Hello World! Hello World! Hello World! Hello World! Hello World!"
                };
                Friends.Insert(0, x);
            }
        }

        private void OnAddTailClicked(object sender, EventArgs e)
        {
            for (int i = 0; i < 3; ++i)
            {
                var x = new Friendx()
                {
                    Image = "https://www.google.com/images/hpp/Chrome_Owned_96x96.png",
                    Nick = "AddTail " + i,
                    Time = "12:00",
                    Message = "Hello World! Hello World! Hello World! Hello World! Hello World!"
                };
                Friends.Add(x);
            }
        }

        private void OnRemoveHeadClicked(object sender, EventArgs e)
        {
            for (int i = 0; i < 3 && Friends.Count > 0; ++i)
            {
                Friends.RemoveAt(0);
            }
        }

        private void OnRemoveTailClicked(object sender, EventArgs e)
        {
            for (int i = 0; i < 3 && Friends.Count > 0; ++i)
            {
                Friends.RemoveAt(Friends.Count - 1);
            }
        }
    }
}