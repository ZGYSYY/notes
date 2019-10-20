FROM openjdk:8-jre
LABEL maintainer ZGY<3030392760@qq.com>
ENV APP_VERSION 1.0.0-SNAPSHOT
#ENV DOCKERIZE_VERSION v0.6.1
#RUN wget https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
#    && tar -C /usr/local/bin -xzvf dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
#    && rm dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz

COPY dockerize-linux-amd64-v0.6.1.tar.gz /usr/local/share/

RUN tar -C /usr/local/bin -xzvf /usr/local/share/dockerize-linux-amd64-v0.6.1.tar.gz  \
    && rm /usr/local/share/dockerize-linux-amd64-v0.6.1.tar.gz

RUN mkdir /app

COPY my-shop-service-user-provider-$APP_VERSION.jar /app/app.jar

ENTRYPOINT ["dockerize", "-timeout", "5m", "-wait", "tcp://192.168.31.66:3306", "java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]

EXPOSE 8501 22222 20881