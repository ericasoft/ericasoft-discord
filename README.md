# Introduction
This repository is a collection of Discord bots made using Springboot and Discord4J.

# Developing
Open as a maven project in your IDE, for example IntelliJ.

# Building
Use maven: ```mvn clean install```

# Deploying with docker-compose
First, the docker image needs to be built:
````mvn clean install -P build-dockers````

Then you can copy the folder /docker to someplace that you can remember.
Make sure to configure the bot token in the /docker/configserver/vchelper.properties file.
You can now deploy with ```docker-compose up -d``` using the docker-compose file you just copied.

# Running
Use maven. First build the whole project. 
Then go to the specific bot you wish to run, and use the following:
```mvn spring-boot:run```

Alternatively, you can specify the path to the sub-project you wish to run, e.g:
```mvn -f ericasoft-discord-vchelper/pom.xml spring-boot:run```