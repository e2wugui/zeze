
#include "Gen/demo/Module1/BAutoValue.hpp"

namespace demo {
namespace Module1 {

BAutoValue::BAutoValue()
{
    Current = 0;
    LocalId = 0;
}

BAutoValue::BAutoValue(int64_t Current_, const std::string& Name_, int64_t LocalId_)
{
    Current = Current_;
    Name = Name_;
    LocalId = LocalId_;
}

void BAutoValue::Assign(const Zeze::Bean& other) {
    Assign(dynamic_cast<const BAutoValue&>(other));
}

void BAutoValue::Assign(const BAutoValue& other) {
    Current = other.Current;
    Name = other.Name;
    LocalId = other.LocalId;
}

BAutoValue& BAutoValue::operator=(const BAutoValue& other) {
    Assign(other);
    return *this;
}

void BAutoValue::Encode(Zeze::ByteBuffer& _o_) const {
    int _i_ = 0;
    {
        auto _x_ = Current;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 1, Zeze::ByteBuffer::INTEGER);
            _o_.WriteLong(_x_);
        }
    }
    {
        const auto& _x_ = Name;
        if (!_x_.empty()) {
            _i_ = _o_.WriteTag(_i_, 2, Zeze::ByteBuffer::BYTES);
            _o_.WriteString(_x_);
        }
    }
    {
        auto _x_ = LocalId;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 3, Zeze::ByteBuffer::INTEGER);
            _o_.WriteLong(_x_);
        }
    }
    _o_.WriteByte(0);
}

void BAutoValue::Decode(Zeze::ByteBuffer& _o_) {
    int _t_ = _o_.ReadByte();
    int _i_ = _o_.ReadTagSize(_t_);
    if (_i_ == 1) {
        Current = _o_.ReadLong(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 2) {
        Name = _o_.ReadString(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 3) {
        LocalId = _o_.ReadLong(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    while (_t_ != 0) {
        _o_.SkipUnknownField(_t_);
        _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
}
}
}
