

MVN_CLEAN    	:= mvn clean
MVN_BUILD    	:= mvn compile -T 4C
MVN_PKG	    	:= mvn package -T 4C -Dmaven.test.skip=true
MVN_TEST		:= mvn test -T 4C


CURRENT_DIR 	:= $(shell pwd)

DOCKER_VOLUME	:= -v $(HOME)/.m2:/root/.m2 -v $(CURRENT_DIR):/ws
DOCKER_IMG		:= maven:3-openjdk-11
DOCKER_RUN 		:= docker run -it --rm -w /ws $(DOCKER_VOLUME) --network host $(DOCKER_IMG)

DOCKER_BUILD 	:= ./build.sh

.PHONY: build test clean package image

build:
	$(DOCKER_RUN) $(MVN_BUILD)

package:
	$(DOCKER_RUN) $(MVN_PKG)

test:
	$(DOCKER_RUN) $(MVN_TEST)

image: package
	$(DOCKER_BUILD) $(tag)

clean:
	$(DOCKER_RUN) $(MVN_CLEAN)