
#include "Gen/demo/Module1/BSimple.hpp"

namespace demo {
namespace Module1 {

BSimple::BSimple()
{
    Int_1 = 0;
    Long2 = 0;
}

BSimple::BSimple(int Int_1_, int64_t Long2_, const std::string& String3_)
{
    Int_1 = Int_1_;
    Long2 = Long2_;
    String3 = String3_;
}

void BSimple::Assign(const Zeze::Bean& other) {
    Assign(dynamic_cast<const BSimple&>(other));
}

void BSimple::Assign(const BSimple& other) {
    Int_1 = other.Int_1;
    Long2 = other.Long2;
    String3 = other.String3;
    Removed.Assign(other.Removed);
}

BSimple& BSimple::operator=(const BSimple& other) {
    Assign(other);
    return *this;
}

void BSimple::Encode(Zeze::ByteBuffer& _o_) const {
    int _i_ = 0;
    {
        auto _x_ = Int_1;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 1, Zeze::ByteBuffer::INTEGER);
            _o_.WriteInt(_x_);
        }
    }
    {
        auto _x_ = Long2;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 2, Zeze::ByteBuffer::INTEGER);
            _o_.WriteLong(_x_);
        }
    }
    {
        const auto& _x_ = String3;
        if (!_x_.empty()) {
            _i_ = _o_.WriteTag(_i_, 3, Zeze::ByteBuffer::BYTES);
            _o_.WriteString(_x_);
        }
    }
    {
        int _a_ = _o_.WriteIndex;
        int _j_ = _o_.WriteTag(_i_, 4, Zeze::ByteBuffer::BEAN);
        int _b_ = _o_.WriteIndex;
        Removed.Encode(_o_);
        if (_b_ + 1 == _o_.WriteIndex)
            _o_.WriteIndex = _a_;
        else
            _i_ = _j_;
    }
    _o_.WriteByte(0);
}

void BSimple::Decode(Zeze::ByteBuffer& _o_) {
    int _t_ = _o_.ReadByte();
    int _i_ = _o_.ReadTagSize(_t_);
    if (_i_ == 1) {
        Int_1 = _o_.ReadInt(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 2) {
        Long2 = _o_.ReadLong(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 3) {
        String3 = _o_.ReadString(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 4) {
        _o_.ReadBean(Removed, _t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    while (_t_ != 0) {
        _o_.SkipUnknownField(_t_);
        _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
}
}
}
