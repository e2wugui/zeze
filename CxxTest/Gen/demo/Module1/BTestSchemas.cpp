
#include "Gen/demo/Module1/BTestSchemas.hpp"

namespace demo {
namespace Module1 {

BTestSchemas::BTestSchemas()
{
    Var1 = 0;
    Var2 = 0;
}

BTestSchemas::BTestSchemas(int64_t Var1_, int64_t Var2_)
{
    Var1 = Var1_;
    Var2 = Var2_;
}

void BTestSchemas::Assign(const Zeze::Bean& other) {
    Assign(dynamic_cast<const BTestSchemas&>(other));
}

void BTestSchemas::Assign(const BTestSchemas& other) {
    Var1 = other.Var1;
    Var2 = other.Var2;
}

BTestSchemas& BTestSchemas::operator=(const BTestSchemas& other) {
    Assign(other);
    return *this;
}

void BTestSchemas::Encode(Zeze::ByteBuffer& _o_) const {
    int _i_ = 0;
    {
        auto _x_ = Var1;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 1, Zeze::ByteBuffer::INTEGER);
            _o_.WriteLong(_x_);
        }
    }
    {
        auto _x_ = Var2;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 2, Zeze::ByteBuffer::INTEGER);
            _o_.WriteLong(_x_);
        }
    }
    _o_.WriteByte(0);
}

void BTestSchemas::Decode(Zeze::ByteBuffer& _o_) {
    int _t_ = _o_.ReadByte();
    int _i_ = _o_.ReadTagSize(_t_);
    if (_i_ == 1) {
        Var1 = _o_.ReadLong(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 2) {
        Var2 = _o_.ReadLong(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    while (_t_ != 0) {
        _o_.SkipUnknownField(_t_);
        _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
}
}
}
