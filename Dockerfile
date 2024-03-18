FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER joey

RUN mkdir -p /joey/server

WORKDIR /joey/server

ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 29223

ADD https://ghproxy.com/https://github.com/joey1029/pter/releases/download/1.0.2/pter-1.0.2.jar ./pter-1.0.2.jar
