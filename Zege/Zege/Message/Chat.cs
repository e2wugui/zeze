using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zege.Message
{
    public interface Chat
    {
        public bool IsYou(string account, long departmentId);
        public Task SendAsync(string message);
        public void Show();
    }
}
