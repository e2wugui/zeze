

cd "%1"

set tag = %2

if "%tag%" == "" (
	for /f %%x in ('"git describe --tags --abbrev=0"') do set tag=%%x
	echo %tag%
)

git checkout "%tag%"

