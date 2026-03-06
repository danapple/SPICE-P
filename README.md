Complete working copy of SPICE-P Code Challenge: Crypto wallet management

There are two essential environment variables:
COINCAP_API_KEY which must be set to your own CoinCap API key
COINCAP_URL which can be set, at this time, to https://rest.coincap.io/v3

Other tunable parameters can be seen in application.yml

build: mvn install
run: java -jar target/spicep-1.0-SNAPSHOT.jar

create a container: docker build .
run: docker run ......
