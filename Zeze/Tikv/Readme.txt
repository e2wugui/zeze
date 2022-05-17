
一，intall
Install GoLang
Install mingw-w64 (windows)
git clone https://github.com/tikv/client-go

二，copy
copy tikv.go client-go/
copy build.bat client-go/
copy tikvbridge.h client-go/
copy tikvbridge.c client-go/

三，clieng-go-1.0 修改，【2.0可能已经不需要。】
Edit client-go\txnkv\kv\memdb_buffer.go
修改下面的代码，判断真正的nil，允许长度为0的数组。
(
// Set associates key with value.
func (m *memDbBuffer) Set(k key.Key, v []byte) error {
	if len(v) == 0 {
		return errors.WithStack(ErrCannotSetNilValue)
	}

)
-->
(
// Set associates key with value.
func (m *memDbBuffer) Set(k key.Key, v []byte) error {
	if v == nil {
		return errors.WithStack(ErrCannotSetNilValue)
	}
)

四，run build.bat
