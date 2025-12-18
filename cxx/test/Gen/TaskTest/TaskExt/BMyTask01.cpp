
#include "Gen/TaskTest/TaskExt/BMyTask01.hpp"

namespace TaskTest {
namespace TaskExt {

BMyTask01::BMyTask01()
{
    Placeholder = 0;
    TaskNum = 0;
}

BMyTask01::BMyTask01(int64_t Placeholder_, int TaskNum_)
{
    Placeholder = Placeholder_;
    TaskNum = TaskNum_;
}

void BMyTask01::Assign(const Zeze::Bean& other) {
    Assign(dynamic_cast<const BMyTask01&>(other));
}

void BMyTask01::Assign(const BMyTask01& other) {
    Placeholder = other.Placeholder;
    TaskNum = other.TaskNum;
}

BMyTask01& BMyTask01::operator=(const BMyTask01& other) {
    Assign(other);
    return *this;
}

void BMyTask01::Encode(Zeze::ByteBuffer& _o_) const {
    int _i_ = 0;
    {
        auto _x_ = Placeholder;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 1, Zeze::ByteBuffer::INTEGER);
            _o_.WriteLong(_x_);
        }
    }
    {
        auto _x_ = TaskNum;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 2, Zeze::ByteBuffer::INTEGER);
            _o_.WriteInt(_x_);
        }
    }
    _o_.WriteByte(0);
}

void BMyTask01::Decode(Zeze::ByteBuffer& _o_) {
    int _t_ = _o_.ReadByte();
    int _i_ = _o_.ReadTagSize(_t_);
    if (_i_ == 1) {
        Placeholder = _o_.ReadLong(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 2) {
        TaskNum = _o_.ReadInt(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    while (_t_ != 0) {
        _o_.SkipUnknownField(_t_);
        _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
}
}
}
