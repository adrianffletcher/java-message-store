`java-message-service` is a JAX-JS RESTful web service which has allows clients to submit messages and receive a SHA-256 hash value and then subsequently provide the hash value to receive the original message.

## Prerequisites

The service is available at Heroku (http://javamessagestore.herokuapp.com/messages/)

For local development and testing you will need:

* Java 1.8 (code has been tested against Java version 1.8.0_131). Download from [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).
* Git (https://git-scm.com/downloads)
* Redis (code has been tested again Redis 3.2.9) (https://redis.io)
* MongoDB (code has been tested against MongoDB 3.4.4) (https://www.mongodb.com/download-center)
* Maven 3.5.0

## Installation

* Clone this git repository
```
    :::term
    $ git clone https://github.com/adrianffletcher/java-message-store
    Cloning into 'java-message-store'...
    remote: Counting objects: 204, done.
    remote: Compressing objects: 100% (88/88), done.
    remote: Total 204 (delta 70), reused 199 (delta 65), pack-reused 0
    Receiving objects: 100% (204/204), 36.36 KiB | 0 bytes/s, done.
    Resolving deltas: 100% (70/70), done.
```
* Install Redis
```
    :::term
    brew install redis
    ==> Downloading https://homebrew.bintray.com/bottles/redis-3.2.9.sierra.bottle.t
    ...
    /usr/local/Cellar/redis/3.2.9: 13 files, 1.7MB
```

* Install MongoDB
```
    :::term
    brew install mongodb
    ==> Downloading https://homebrew.bintray.com/bottles/mongodb-3.4.4.sierra.bottle.tar.gz
    Already downloaded: /Users/adrianfletcher/Library/Caches/Homebrew/mongodb-3.4.4.sierra.bottle.tar.gz
    ==> Pouring mongodb-3.4.4.sierra.bottle.tar.gz
    ==> Using the sandbox
    ==> Caveats
    To have launchd start mongodb now and restart at login:
      brew services start mongodb
    Or, if you don't want/need a background service you can just run:
      mongod --config /usr/local/etc/mongod.conf
    ==> Summary
    /usr/local/Cellar/mongodb/3.4.4: 18 files, 266.3MB
```

* Install Maven
```
    :::term
    brew install maven
    ==> Using the sandbox
    ==> Downloading https://www.apache.org/dyn/closer.cgi?path=maven/maven-3/3.5.0/binaries/apache-maven-3.5.0-bin.tar.gz
    Already downloaded: /Users/adrianfletcher/Library/Caches/Homebrew/maven-3.5.0.tar.gz
    /usr/local/Cellar/maven/3.5.0: 106 files, 9.8MB, built in 1 second
```

* Build
```
    :::term
    $ cd java-message-store
    mvn clean install
    [INFO] Scanning for projects...
    [INFO]
    [INFO] ------------------------------------------------------------------------
    [INFO] Building message-store 0.1
    [INFO] ------------------------------------------------------------------------
    ...
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
```

* Run Redis
```
    :::term
    $ redis-server redis.conf
    ...
    13302:M 11 Jun 14:59:57.149 * The server is now ready to accept connections on port 6379
```

* Run MongoDB (you may need to run this as root)
```
    :::term
    $ brew services start mongodb
    ==> Successfully started `mongodb` (label: homebrew.mxcl.mongodb)
```

* Run Jetty Server
```
    :::term
    $ export MONGODB_URI=mongodb://localhost:27017/javamessageservice
    $ export REDIS_URL=redis://localhost:6379
    $ java -cp target/classes:target/dependency/* com.example.JettyLauncher
    017-06-11 15:51:56.471:INFO::main: Logging initialized @152ms to org.eclipse.jetty.util.log.StdErrLog
    2017-06-11 15:51:56.653:INFO:oejs.Server:main: jetty-9.4.5.v20170502
    ...
    2017-06-11 15:51:57.574:INFO:oejs.Server:main: Started @1257ms
```
