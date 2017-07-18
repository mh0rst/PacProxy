# PacProxy
This tool provides a HTTP proxy server that routes requests based on the decisions of a given PAC file.

A PAC file contains a JavaScript function that decides based on a URL and the URLs host name whether to pass the request through a proxy or to process the request directly. PAC files usually can be found in enterprise networks and can be used for example to distinguish between internal hosts (which should be accessed directly) and external hosts (which should be accessed through a proxy). Unfortunately there are many applications that do not support or interpret PAC files, leaving them with no usable networking support in use cases that need both access to internal and external hosts.

These applications can be fixed with PacProxy: Simply configure the applications to use the PacProxy port as HTTP proxy and let PacProxy decide, whether to use a proxy or not for a given request.

## Build and installation
You need a JDK 8 (Oracle or OpenJDK will do) and Apache Maven to build PacProxy.
1. Clone the repo: ```git clone https://github.com/mh0rst/PacProxy```
2. Run maven: ```mvn package```
3. Copy artifacts and dependencies to a dedicated directory: ```cp -r target/PacProxy*.jar target/lib /path/to/pacproxy```

## Usage
Copy your PAC file to the PacProxy directory. PacProxy does not support automatic download or refresh of PAC files at the moment.
Then, run inside the PacProxy directory:

	java -jar PacProxy-1.0.0-SNAPSHOT.jar -p proxy.pac

PacProxy will listen to port 3128 on localhost by default, you may bind it to another port or interface by using the ```-b``` parameter:

	java -jar PacProxy-1.0.0-SNAPSHOT.jar -p proxy.pac -b 0.0.0.0:1337

## Acknowledgments
The development of PacProxy would be a lot harder without this software:
* [LittleProxy](https://github.com/adamfisk/LittleProxy)
* [The Netty project](https://netty.io/)
* [SLF4J](https://www.slf4j.org/)

Thanks a lot!

## Open tasks
* Unit tests
* PAC file examples
* SOCKS proxy support (partially implemented, see socks-support branch)
* Automatic PAC file discovery/download/refresh
* Proper IPv6 proxy support