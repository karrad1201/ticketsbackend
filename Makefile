JAVA_HOME ?= /home/karrad/.jdks/openjdk-26
MAVEN_BIN ?= /opt/intellij-idea-ultimate-edition/plugins/maven/lib/maven3/bin
PATH := $(MAVEN_BIN):$(JAVA_HOME)/bin:$(PATH)

.PHONY: test coverage

test:
	mvn test

coverage:
	mvn test jacoco:report jacoco:check@check
