#pragma once

#include "zeze/cxx/Bean.h"

namespace demo {
namespace Module1 {
// beankey的注释
class Key : public Zeze::Serializable {
public:
    static const int Enum1 = 4; // enum的注释

    short S; // com 2
    std::string Str; // com 2

    // for decode only
    Key() {
        S = 1;
    }

    void Assign(const Key& other) {
        S = other.S;
        Str = other.Str;
    }

    Key& operator=(const Key& other) {
        Assign(other);
        return *this;
    }

    Key(short S_, const std::string& Str_) {
        S = S_;
        Str = Str_;
    }

    virtual void Encode(Zeze::ByteBuffer& _o_) const override {
        int _i_ = 0;
        {
            auto _x_ = S;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, Zeze::ByteBuffer::INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            const auto& _x_ = Str;
            if (!_x_.empty()) {
                _i_ = _o_.WriteTag(_i_, 2, Zeze::ByteBuffer::BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    virtual void Decode(Zeze::ByteBuffer& _o_) override {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            S = _o_.ReadShort(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            S = 0;
        if (_i_ == 2) {
            Str = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    int CompareTo(const Key& _o_) const {
        if (&_o_ == this)
            return 0;
        int _c_;
        _c_ = Zeze::Short::Compare(S, _o_.S);
        if (_c_ != 0)
            return _c_;
        _c_ = Zeze::String::Compare(Str, _o_.Str);
        if (_c_ != 0)
            return _c_;
        return _c_;
    }

    bool operator<(const Key& _o_) const {
        return CompareTo(_o_) < 0;
    }
};
}
}
