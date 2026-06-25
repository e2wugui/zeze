
copy client.xml server\lib\
copy linkd.xml server\lib\
copy server.xml server\lib\
copy log4j2.xml server\lib\
copy client\build\libs\client-0.0.1-SNAPSHOT.jar server\lib\
copy linkd\build\libs\linkd-0.0.1-SNAPSHOT.jar server\lib\
copy server\build\libs\server-0.0.1-SNAPSHOT.jar server\lib\
pscp -P 29010 -r server\lib lichenghua@10.12.60.70:
