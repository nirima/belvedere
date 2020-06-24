From openjdk:8

COPY bin/belvedere-cli.jar /bin

RUN mkdir /api

VOLUME ["/api"]

ENTRYPOINT ["java","-jar","/bin/belvedere-cli.jar"]

