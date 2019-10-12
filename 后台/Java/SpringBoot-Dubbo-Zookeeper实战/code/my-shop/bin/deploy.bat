# 批量上传私服
cd ..
cd my-shop-dependencies
call mvn deploy

cd ..
cd my-shop-commons
call mvn deploy

cd ..
cd my-shop-commons-domain
call mvn deploy

cd ..
cd my-shop-commons-mapper
call mvn deploy

cd ..
cd my-shop-service-user-api
call mvn deploy

cd ..
cd my-shop-commons-dubbo
call mvn deploy

cd ..
cd my-shop-static-backend
call mvn deploy