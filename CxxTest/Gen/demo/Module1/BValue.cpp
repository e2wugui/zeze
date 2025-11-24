
#include "Gen/demo/Module1/BValue.hpp"
#include "demo/Module1/ModuleModule1.h"

namespace demo {
namespace Module1 {

Zeze::DynamicBean BValue::constructDynamicBean_Dynamic14() {
    return Zeze::DynamicBean(BValue::GetSpecialTypeIdFromBean_14, BValue::CreateBeanFromSpecialTypeId_14);
}

int64_t BValue::GetSpecialTypeIdFromBean_14(const Zeze::Bean* bean) {
    auto _typeId_ = bean->TypeId();
    if (_typeId_ == Zeze::EmptyBean::TYPEID)
        return Zeze::EmptyBean::TYPEID;
    if (_typeId_ == -410057899348847631L)
        return 1LL; // demo::Bean1
    if (_typeId_ == 4513771153805810055L)
        return 2LL; // demo::Module1::BSimple
    if (_typeId_ == -1770669781233931946L)
        return 3LL; // demo::Module1::BItem
    throw std::runtime_error("Unknown Bean! dynamic@demo::Module1::BValue:dynamic14");
}

Zeze::Bean* BValue::CreateBeanFromSpecialTypeId_14(int64_t typeId) {
    if (typeId == 1LL)
        return new demo::Bean1();
    if (typeId == 2LL)
        return new demo::Module1::BSimple();
    if (typeId == 3LL)
        return new demo::Module1::BItem();
    return nullptr;
}

Zeze::DynamicBean BValue::constructDynamicBean_Dynamic23() {
    return Zeze::DynamicBean(demo::Module1::ModuleModule1::getSpecialTypeIdFromBean, demo::Module1::ModuleModule1::createBeanFromSpecialTypeId);
}

int64_t BValue::GetSpecialTypeIdFromBean_23(const Zeze::Bean* bean) {
    return demo::Module1::ModuleModule1::getSpecialTypeIdFromBean(bean);
}

Zeze::Bean* BValue::CreateBeanFromSpecialTypeId_23(int64_t typeId) {
    return demo::Module1::ModuleModule1::createBeanFromSpecialTypeId(typeId);
}

Zeze::DynamicBean BValue::constructDynamicBean_Map26() {
    return Zeze::DynamicBean(BValue::GetSpecialTypeIdFromBean_26, BValue::CreateBeanFromSpecialTypeId_26);
}

int64_t BValue::GetSpecialTypeIdFromBean_26(const Zeze::Bean* bean) {
    auto _typeId_ = bean->TypeId();
    if (_typeId_ == Zeze::EmptyBean::TYPEID)
        return Zeze::EmptyBean::TYPEID;
    if (_typeId_ == 4513771153805810055L)
        return 4513771153805810055LL; // demo::Module1::BSimple
    throw std::runtime_error("Unknown Bean! dynamic@demo::Module1::BValue:map26");
}

Zeze::Bean* BValue::CreateBeanFromSpecialTypeId_26(int64_t typeId) {
    if (typeId == 4513771153805810055LL)
        return new demo::Module1::BSimple();
    return nullptr;
}

Zeze::DynamicBean BValue::constructDynamicBean_Dynamic27() {
    return Zeze::DynamicBean(BValue::GetSpecialTypeIdFromBean_27, BValue::CreateBeanFromSpecialTypeId_27);
}

int64_t BValue::GetSpecialTypeIdFromBean_27(const Zeze::Bean* bean) {
    auto _typeId_ = bean->TypeId();
    if (_typeId_ == Zeze::EmptyBean::TYPEID)
        return Zeze::EmptyBean::TYPEID;
    if (_typeId_ == 4513771153805810055L)
        return 4513771153805810055LL; // demo::Module1::BSimple
    throw std::runtime_error("Unknown Bean! dynamic@demo::Module1::BValue:dynamic27");
}

Zeze::Bean* BValue::CreateBeanFromSpecialTypeId_27(int64_t typeId) {
    if (typeId == 4513771153805810055LL)
        return new demo::Module1::BSimple();
    return nullptr;
}

Zeze::DynamicBean BValue::constructDynamicBean_List43() {
    return Zeze::DynamicBean(BValue::GetSpecialTypeIdFromBean_43, BValue::CreateBeanFromSpecialTypeId_43);
}

int64_t BValue::GetSpecialTypeIdFromBean_43(const Zeze::Bean* bean) {
    auto _typeId_ = bean->TypeId();
    if (_typeId_ == Zeze::EmptyBean::TYPEID)
        return Zeze::EmptyBean::TYPEID;
    if (_typeId_ == 4513771153805810055L)
        return 4513771153805810055LL; // demo::Module1::BSimple
    throw std::runtime_error("Unknown Bean! dynamic@demo::Module1::BValue:list43");
}

Zeze::Bean* BValue::CreateBeanFromSpecialTypeId_43(int64_t typeId) {
    if (typeId == 4513771153805810055LL)
        return new demo::Module1::BSimple();
    return nullptr;
}

BValue::BValue()
    : Dynamic14(GetSpecialTypeIdFromBean_14, CreateBeanFromSpecialTypeId_14)
    , Dynamic23(GetSpecialTypeIdFromBean_23, CreateBeanFromSpecialTypeId_23)
    , Dynamic27(GetSpecialTypeIdFromBean_27, CreateBeanFromSpecialTypeId_27)
{
    Int_1 = 0;
    Long2 = 0;
    Bool4 = false;
    Short5 = 0;
    Float6 = 0.0f;
    Double7 = 0.0;
    Byte13 = 0;
    Version = 0;
}

BValue::BValue(int Int_1_, int64_t Long2_, const std::string& String3_, bool Bool4_, short Short5_, float Float6_, double Double7_, const std::string& Bytes8_, char Byte13_, const Zeze::Vector2& Vector2_, const Zeze::Vector2Int& Vector2Int_, const Zeze::Vector3& Vector3_, const Zeze::Vector3Int& Vector3Int_, const Zeze::Vector4& Vector4_, const Zeze::Quaternion& Quaternion_, demo::Module1::Key Key28_, const std::string& JsonObject_, const std::string& JsonArray_)
    : Dynamic14(GetSpecialTypeIdFromBean_14, CreateBeanFromSpecialTypeId_14)
    , Dynamic23(GetSpecialTypeIdFromBean_23, CreateBeanFromSpecialTypeId_23)
    , Dynamic27(GetSpecialTypeIdFromBean_27, CreateBeanFromSpecialTypeId_27)
{
    Int_1 = Int_1_;
    Long2 = Long2_;
    String3 = String3_;
    Bool4 = Bool4_;
    Short5 = Short5_;
    Float6 = Float6_;
    Double7 = Double7_;
    Bytes8 = Bytes8_;
    Byte13 = Byte13_;
    Vector2 = Vector2_;
    Vector2Int = Vector2Int_;
    Vector3 = Vector3_;
    Vector3Int = Vector3Int_;
    Vector4 = Vector4_;
    Quaternion = Quaternion_;
    Key28 = Key28_;
    JsonObject = JsonObject_;
    JsonArray = JsonArray_;
}

void BValue::Assign(const Zeze::Bean& other) {
    Assign(dynamic_cast<const BValue&>(other));
}

void BValue::Assign(const BValue& other) {
    Int_1 = other.Int_1;
    Long2 = other.Long2;
    String3 = other.String3;
    Bool4 = other.Bool4;
    Short5 = other.Short5;
    Float6 = other.Float6;
    Double7 = other.Double7;
    Bytes8 = other.Bytes8;
    List9 = other.List9;
    Set10 = other.Set10;
    Map11 = other.Map11;
    Bean12.Assign(other.Bean12);
    Byte13 = other.Byte13;
    Dynamic14.Assign(other.Dynamic14);
    Map15 = other.Map15;
    Map16 = other.Map16;
    Vector2 = other.Vector2;
    Vector2Int = other.Vector2Int;
    Vector3 = other.Vector3;
    Vector3Int = other.Vector3Int;
    Vector4 = other.Vector4;
    Quaternion = other.Quaternion;
    Dynamic23.Assign(other.Dynamic23);
    ListVector2Int = other.ListVector2Int;
    Map25 = other.Map25;
    Map26 = other.Map26;
    Dynamic27.Assign(other.Dynamic27);
    Key28.Assign(other.Key28);
    Array29 = other.Array29;
    List30 = other.List30;
    List31 = other.List31;
    List32 = other.List32;
    List33 = other.List33;
    List34 = other.List34;
    List35 = other.List35;
    List36 = other.List36;
    List37 = other.List37;
    Set38 = other.Set38;
    Set39 = other.Set39;
    Map40 = other.Map40;
    Map41 = other.Map41;
    Map42Recursive = other.Map42Recursive;
    List43 = other.List43;
    JsonObject = other.JsonObject;
    JsonArray = other.JsonArray;
    Version = other.Version;
    LongList = other.LongList;
}

BValue& BValue::operator=(const BValue& other) {
    Assign(other);
    return *this;
}

void BValue::Encode(Zeze::ByteBuffer& _o_) const {
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
        auto _x_ = Bool4;
        if (_x_) {
            _i_ = _o_.WriteTag(_i_, 4, Zeze::ByteBuffer::INTEGER);
            _o_.WriteByte(1);
        }
    }
    {
        auto _x_ = Short5;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 5, Zeze::ByteBuffer::INTEGER);
            _o_.WriteInt(_x_);
        }
    }
    {
        auto _x_ = Float6;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 6, Zeze::ByteBuffer::FLOAT);
            _o_.WriteFloat(_x_);
        }
    }
    {
        auto _x_ = Double7;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 7, Zeze::ByteBuffer::DOUBLE);
            _o_.WriteDouble(_x_);
        }
    }
    {
        const auto& _x_ = Bytes8;
        if (_x_.size() != 0) {
            _i_ = _o_.WriteTag(_i_, 8, Zeze::ByteBuffer::BYTES);
            _o_.WriteBinary(_x_);
        }
    }
    {
        const auto& _x_ = List9;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 9, Zeze::ByteBuffer::LIST);
            _o_.WriteListType(_n_, Zeze::ByteBuffer::BEAN);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                (*it).Encode(_o_);
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = Set10;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 10, Zeze::ByteBuffer::LIST);
            _o_.WriteListType(_n_, Zeze::ByteBuffer::INTEGER);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                _o_.WriteInt((*it));
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = Map11;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 11, Zeze::ByteBuffer::MAP);
            _o_.WriteMapType(_n_, Zeze::ByteBuffer::INTEGER, Zeze::ByteBuffer::BEAN);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                _o_.WriteLong(it->first);
                it->second.Encode(_o_);
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        int _a_ = _o_.WriteIndex;
        int _j_ = _o_.WriteTag(_i_, 12, Zeze::ByteBuffer::BEAN);
        int _b_ = _o_.WriteIndex;
        Bean12.Encode(_o_);
        if (_b_ + 1 == _o_.WriteIndex)
            _o_.WriteIndex = _a_;
        else
            _i_ = _j_;
    }
    {
        auto _x_ = Byte13;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 13, Zeze::ByteBuffer::INTEGER);
            _o_.WriteInt(_x_);
        }
    }
    {
        const auto& _x_ = Dynamic14;
        if (!_x_.Empty()) {
            _i_ = _o_.WriteTag(_i_, 14, Zeze::ByteBuffer::DYNAMIC);
            _x_.Encode(_o_);
        }
    }
    {
        const auto& _x_ = Map15;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 15, Zeze::ByteBuffer::MAP);
            _o_.WriteMapType(_n_, Zeze::ByteBuffer::INTEGER, Zeze::ByteBuffer::INTEGER);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                _o_.WriteLong(it->first);
                _o_.WriteLong(it->second);
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = Map16;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 16, Zeze::ByteBuffer::MAP);
            _o_.WriteMapType(_n_, Zeze::ByteBuffer::BEAN, Zeze::ByteBuffer::BEAN);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                it->first.Encode(_o_);
                it->second.Encode(_o_);
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = Vector2;
        if (!_x_.IsZero()) {
            _i_ = _o_.WriteTag(_i_, 17, Zeze::ByteBuffer::VECTOR2);
            _o_.WriteVector2(_x_);
        }
    }
    {
        const auto& _x_ = Vector2Int;
        if (!_x_.IsZero()) {
            _i_ = _o_.WriteTag(_i_, 18, Zeze::ByteBuffer::VECTOR2INT);
            _o_.WriteVector2Int(_x_);
        }
    }
    {
        const auto& _x_ = Vector3;
        if (!_x_.IsZero()) {
            _i_ = _o_.WriteTag(_i_, 19, Zeze::ByteBuffer::VECTOR3);
            _o_.WriteVector3(_x_);
        }
    }
    {
        const auto& _x_ = Vector3Int;
        if (!_x_.IsZero()) {
            _i_ = _o_.WriteTag(_i_, 20, Zeze::ByteBuffer::VECTOR3INT);
            _o_.WriteVector3Int(_x_);
        }
    }
    {
        const auto& _x_ = Vector4;
        if (!_x_.IsZero()) {
            _i_ = _o_.WriteTag(_i_, 21, Zeze::ByteBuffer::VECTOR4);
            _o_.WriteVector4(_x_);
        }
    }
    {
        const auto& _x_ = Quaternion;
        if (!_x_.IsZero()) {
            _i_ = _o_.WriteTag(_i_, 22, Zeze::ByteBuffer::VECTOR4);
            _o_.WriteQuaternion(_x_);
        }
    }
    {
        const auto& _x_ = Dynamic23;
        if (!_x_.Empty()) {
            _i_ = _o_.WriteTag(_i_, 23, Zeze::ByteBuffer::DYNAMIC);
            _x_.Encode(_o_);
        }
    }
    {
        const auto& _x_ = ListVector2Int;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 24, Zeze::ByteBuffer::LIST);
            _o_.WriteListType(_n_, Zeze::ByteBuffer::VECTOR2INT);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                (*it).Encode(_o_);
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = Map25;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 25, Zeze::ByteBuffer::MAP);
            _o_.WriteMapType(_n_, Zeze::ByteBuffer::BEAN, Zeze::ByteBuffer::BEAN);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                it->first.Encode(_o_);
                it->second.Encode(_o_);
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = Map26;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 26, Zeze::ByteBuffer::MAP);
            _o_.WriteMapType(_n_, Zeze::ByteBuffer::BEAN, Zeze::ByteBuffer::DYNAMIC);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                it->first.Encode(_o_);
                it->second.Encode(_o_);
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = Dynamic27;
        if (!_x_.Empty()) {
            _i_ = _o_.WriteTag(_i_, 27, Zeze::ByteBuffer::DYNAMIC);
            _x_.Encode(_o_);
        }
    }
    {
        int _a_ = _o_.WriteIndex;
        int _j_ = _o_.WriteTag(_i_, 28, Zeze::ByteBuffer::BEAN);
        int _b_ = _o_.WriteIndex;
        Key28.Encode(_o_);
        if (_b_ + 1 == _o_.WriteIndex)
            _o_.WriteIndex = _a_;
        else
            _i_ = _j_;
    }
    {
        const auto& _x_ = Array29;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 29, Zeze::ByteBuffer::LIST);
            _o_.WriteListType(_n_, Zeze::ByteBuffer::FLOAT);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                _o_.WriteFloat((*it));
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = List30;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 30, Zeze::ByteBuffer::LIST);
            _o_.WriteListType(_n_, Zeze::ByteBuffer::INTEGER);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                _o_.WriteInt((*it));
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = List31;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 31, Zeze::ByteBuffer::LIST);
            _o_.WriteListType(_n_, Zeze::ByteBuffer::INTEGER);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                _o_.WriteLong((*it));
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = List32;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 32, Zeze::ByteBuffer::LIST);
            _o_.WriteListType(_n_, Zeze::ByteBuffer::FLOAT);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                _o_.WriteFloat((*it));
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = List33;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 33, Zeze::ByteBuffer::LIST);
            _o_.WriteListType(_n_, Zeze::ByteBuffer::VECTOR2);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                (*it).Encode(_o_);
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = List34;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 34, Zeze::ByteBuffer::LIST);
            _o_.WriteListType(_n_, Zeze::ByteBuffer::VECTOR3);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                (*it).Encode(_o_);
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = List35;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 35, Zeze::ByteBuffer::LIST);
            _o_.WriteListType(_n_, Zeze::ByteBuffer::VECTOR4);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                (*it).Encode(_o_);
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = List36;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 36, Zeze::ByteBuffer::LIST);
            _o_.WriteListType(_n_, Zeze::ByteBuffer::VECTOR2INT);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                (*it).Encode(_o_);
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = List37;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 37, Zeze::ByteBuffer::LIST);
            _o_.WriteListType(_n_, Zeze::ByteBuffer::VECTOR3INT);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                (*it).Encode(_o_);
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = Set38;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 38, Zeze::ByteBuffer::LIST);
            _o_.WriteListType(_n_, Zeze::ByteBuffer::INTEGER);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                _o_.WriteInt((*it));
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = Set39;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 39, Zeze::ByteBuffer::LIST);
            _o_.WriteListType(_n_, Zeze::ByteBuffer::INTEGER);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                _o_.WriteLong((*it));
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = Map40;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 40, Zeze::ByteBuffer::MAP);
            _o_.WriteMapType(_n_, Zeze::ByteBuffer::INTEGER, Zeze::ByteBuffer::INTEGER);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                _o_.WriteInt(it->first);
                _o_.WriteInt(it->second);
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = Map41;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 41, Zeze::ByteBuffer::MAP);
            _o_.WriteMapType(_n_, Zeze::ByteBuffer::INTEGER, Zeze::ByteBuffer::BEAN);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                _o_.WriteLong(it->first);
                it->second.Encode(_o_);
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = Map42Recursive;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 42, Zeze::ByteBuffer::MAP);
            _o_.WriteMapType(_n_, Zeze::ByteBuffer::INTEGER, Zeze::ByteBuffer::BEAN);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                _o_.WriteLong(it->first);
                it->second.Encode(_o_);
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = List43;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 43, Zeze::ByteBuffer::LIST);
            _o_.WriteListType(_n_, Zeze::ByteBuffer::DYNAMIC);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                (*it).Encode(_o_);
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    {
        const auto& _x_ = JsonObject;
        if (!_x_.empty()) {
            _i_ = _o_.WriteTag(_i_, 44, Zeze::ByteBuffer::BYTES);
            _o_.WriteString(_x_);
        }
    }
    {
        const auto& _x_ = JsonArray;
        if (!_x_.empty()) {
            _i_ = _o_.WriteTag(_i_, 45, Zeze::ByteBuffer::BYTES);
            _o_.WriteString(_x_);
        }
    }
    {
        auto _x_ = Version;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 50, Zeze::ByteBuffer::INTEGER);
            _o_.WriteLong(_x_);
        }
    }
    {
        const auto& _x_ = LongList;
        auto _n_ = _x_.size();
        if (_n_ != 0) {
            _i_ = _o_.WriteTag(_i_, 99, Zeze::ByteBuffer::LIST);
            _o_.WriteListType(_n_, Zeze::ByteBuffer::INTEGER);
            for (auto it = _x_.begin(); it != _x_.end(); ++it) {
                _o_.WriteLong((*it));
                _n_--;
            }
            if (_n_ != 0)
                throw std::exception();
        }
    }
    _o_.WriteByte(0);
}

void BValue::Decode(Zeze::ByteBuffer& _o_) {
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
        Bool4 = _o_.ReadBool(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 5) {
        Short5 = _o_.ReadShort(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 6) {
        Float6 = _o_.ReadFloat(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 7) {
        Double7 = _o_.ReadDouble(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 8) {
        Bytes8 = _o_.ReadBinary(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 9) {
        auto& _x_ = List9;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::LIST) {
            for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                demo::Bean1 _e_;
                _e_.Decode(_o_);
                _x_.push_back(_e_);
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 10) {
        auto& _x_ = Set10;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::LIST) {
            for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                int _e_;
                _e_ = _o_.ReadInt();
                _x_.insert(_e_);
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 11) {
        auto& _x_ = Map11;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::MAP) {
            int _s_ = (_t_ = _o_.ReadByte()) >> Zeze::ByteBuffer::TAG_SHIFT;
            for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                int64_t _k_;
                _k_ = _o_.ReadLong();
                demo::Module2::BValue _v_;
                _v_.Decode(_o_);
                _x_[_k_] = _v_;
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Map");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 12) {
        _o_.ReadBean(Bean12, _t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 13) {
        Byte13 = _o_.ReadByte(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 14) {
        _o_.ReadDynamic(Dynamic14, _t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 15) {
        auto& _x_ = Map15;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::MAP) {
            int _s_ = (_t_ = _o_.ReadByte()) >> Zeze::ByteBuffer::TAG_SHIFT;
            for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                int64_t _k_;
                _k_ = _o_.ReadLong();
                int64_t _v_;
                _v_ = _o_.ReadLong();
                _x_[_k_] = _v_;
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Map");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 16) {
        auto& _x_ = Map16;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::MAP) {
            int _s_ = (_t_ = _o_.ReadByte()) >> Zeze::ByteBuffer::TAG_SHIFT;
            for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                demo::Module1::Key _k_;
                _k_.Decode(_o_);
                demo::Module1::BSimple _v_;
                _v_.Decode(_o_);
                _x_[_k_] = _v_;
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Map");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 17) {
        Vector2 = _o_.ReadVector2(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 18) {
        Vector2Int = _o_.ReadVector2Int(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 19) {
        Vector3 = _o_.ReadVector3(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 20) {
        Vector3Int = _o_.ReadVector3Int(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 21) {
        Vector4 = _o_.ReadVector4(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 22) {
        Quaternion = _o_.ReadQuaternion(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 23) {
        _o_.ReadDynamic(Dynamic23, _t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 24) {
        auto& _x_ = ListVector2Int;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::LIST) {
            for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                Zeze::Vector2Int _e_;
                _e_ = _o_.ReadVector2Int(_t_);
                _x_.push_back(_e_);
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 25) {
        auto& _x_ = Map25;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::MAP) {
            int _s_ = (_t_ = _o_.ReadByte()) >> Zeze::ByteBuffer::TAG_SHIFT;
            for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                demo::Module1::Key _k_;
                _k_.Decode(_o_);
                demo::Module1::BSimple _v_;
                _v_.Decode(_o_);
                _x_[_k_] = _v_;
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Map");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 26) {
        auto& _x_ = Map26;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::MAP) {
            int _s_ = (_t_ = _o_.ReadByte()) >> Zeze::ByteBuffer::TAG_SHIFT;
            for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                demo::Module1::Key _k_;
                _k_.Decode(_o_);
                Zeze::DynamicBean _v_(BValue::GetSpecialTypeIdFromBean_26, BValue::CreateBeanFromSpecialTypeId_26);
                _o_.ReadDynamic(_v_, _t_);
                _x_[_k_] = _v_;
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Map");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 27) {
        _o_.ReadDynamic(Dynamic27, _t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 28) {
        _o_.ReadBean(Key28, _t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 29) {
        auto& _x_ = Array29;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::LIST) {
            for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                float _e_;
                _e_ = _o_.ReadFloat();
                _x_.push_back(_e_);
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 30) {
        auto& _x_ = List30;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::LIST) {
            for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                int _e_;
                _e_ = _o_.ReadInt();
                _x_.push_back(_e_);
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 31) {
        auto& _x_ = List31;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::LIST) {
            for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                int64_t _e_;
                _e_ = _o_.ReadLong();
                _x_.push_back(_e_);
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 32) {
        auto& _x_ = List32;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::LIST) {
            for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                float _e_;
                _e_ = _o_.ReadFloat();
                _x_.push_back(_e_);
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 33) {
        auto& _x_ = List33;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::LIST) {
            for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                Zeze::Vector2 _e_;
                _e_ = _o_.ReadVector2(_t_);
                _x_.push_back(_e_);
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 34) {
        auto& _x_ = List34;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::LIST) {
            for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                Zeze::Vector3 _e_;
                _e_ = _o_.ReadVector3(_t_);
                _x_.push_back(_e_);
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 35) {
        auto& _x_ = List35;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::LIST) {
            for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                Zeze::Vector4 _e_;
                _e_ = _o_.ReadVector4(_t_);
                _x_.push_back(_e_);
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 36) {
        auto& _x_ = List36;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::LIST) {
            for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                Zeze::Vector2Int _e_;
                _e_ = _o_.ReadVector2Int(_t_);
                _x_.push_back(_e_);
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 37) {
        auto& _x_ = List37;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::LIST) {
            for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                Zeze::Vector3Int _e_;
                _e_ = _o_.ReadVector3Int(_t_);
                _x_.push_back(_e_);
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 38) {
        auto& _x_ = Set38;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::LIST) {
            for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                int _e_;
                _e_ = _o_.ReadInt();
                _x_.insert(_e_);
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 39) {
        auto& _x_ = Set39;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::LIST) {
            for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                int64_t _e_;
                _e_ = _o_.ReadLong();
                _x_.insert(_e_);
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 40) {
        auto& _x_ = Map40;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::MAP) {
            int _s_ = (_t_ = _o_.ReadByte()) >> Zeze::ByteBuffer::TAG_SHIFT;
            for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                int _k_;
                _k_ = _o_.ReadInt();
                int _v_;
                _v_ = _o_.ReadInt();
                _x_[_k_] = _v_;
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Map");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 41) {
        auto& _x_ = Map41;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::MAP) {
            int _s_ = (_t_ = _o_.ReadByte()) >> Zeze::ByteBuffer::TAG_SHIFT;
            for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                int64_t _k_;
                _k_ = _o_.ReadLong();
                demo::Module1::BSimple _v_;
                _v_.Decode(_o_);
                _x_[_k_] = _v_;
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Map");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 42) {
        auto& _x_ = Map42Recursive;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::MAP) {
            int _s_ = (_t_ = _o_.ReadByte()) >> Zeze::ByteBuffer::TAG_SHIFT;
            for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                int64_t _k_;
                _k_ = _o_.ReadLong();
                demo::Module1::BValue _v_;
                _v_.Decode(_o_);
                _x_[_k_] = _v_;
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Map");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 43) {
        auto& _x_ = List43;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::LIST) {
            for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                Zeze::DynamicBean _e_(BValue::GetSpecialTypeIdFromBean_43, BValue::CreateBeanFromSpecialTypeId_43);
                _o_.ReadDynamic(_e_, _t_);
                _x_.push_back(_e_);
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 44) {
        JsonObject = _o_.ReadString(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 45) {
        JsonArray = _o_.ReadString(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    while ((_t_ & 0xff) > 1 && _i_ < 50) {
        _o_.SkipUnknownField(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 50) {
        Version = _o_.ReadLong(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    while ((_t_ & 0xff) > 1 && _i_ < 99) {
        _o_.SkipUnknownField(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 99) {
        auto& _x_ = LongList;
        _x_.clear();
        if ((_t_ & Zeze::ByteBuffer::TAG_MASK) == Zeze::ByteBuffer::LIST) {
            for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                int64_t _e_;
                _e_ = _o_.ReadLong();
                _x_.push_back(_e_);
            }
        } else
            _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    while (_t_ != 0) {
        _o_.SkipUnknownField(_t_);
        _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
}
}
}
