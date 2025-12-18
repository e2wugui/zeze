
#include "Gen/demo/M6/M7/B67.hpp"

namespace demo {
namespace M6 {
namespace M7 {

B67::B67()
{
}

B67::B67(const std::string& Name_)
{
    Name = Name_;
}

void B67::Assign(const Zeze::Bean& other) {
    Assign(dynamic_cast<const B67&>(other));
}

void B67::Assign(const B67& other) {
    Name = other.Name;
}

B67& B67::operator=(const B67& other) {
    Assign(other);
    return *this;
}

void B67::Encode(Zeze::ByteBuffer& _o_) const {
    int _i_ = 0;
    {
        const auto& _x_ = Name;
        if (!_x_.empty()) {
            _i_ = _o_.WriteTag(_i_, 1, Zeze::ByteBuffer::BYTES);
            _o_.WriteString(_x_);
        }
    }
    _o_.WriteByte(0);
}

void B67::Decode(Zeze::ByteBuffer& _o_) {
    int _t_ = _o_.ReadByte();
    int _i_ = _o_.ReadTagSize(_t_);
    if (_i_ == 1) {
        Name = _o_.ReadString(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    while (_t_ != 0) {
        _o_.SkipUnknownField(_t_);
        _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
}
}
}
}
