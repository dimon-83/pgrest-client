MVN ?= mvn
GPG_KEYID ?=
GPG_PASSPHRASE ?=
MAVEN_SETTINGS ?=
ALT_DEPLOY_REPO ?=

GPG_PASSPHRASE_FILE ?= .mvn/gpg-passphrase.txt
ifeq ($(strip $(GPG_PASSPHRASE)),)
GPG_PASSPHRASE := $(shell cat $(GPG_PASSPHRASE_FILE) 2>/dev/null)
endif

# Optional CLI args (skip when empty)
SETTINGS_OPT := $(if $(MAVEN_SETTINGS),-s $(MAVEN_SETTINGS),)

.PHONY: help clean install sign-install deploy-local deploy-central deploy-ossrh publish-central

help:
	echo Usage:
	echo make install
	echo make sign-install GPG_KEYID=... GPG_PASSPHRASE=...
	echo make deploy-local ALT_DEPLOY_REPO=local::default::file:///E:/workspace/pgrest-client/.local-repo
	echo make deploy-central MAVEN_SETTINGS=~/.m2/settings.xml GPG_KEYID=... GPG_PASSPHRASE=...
	echo make deploy-ossrh MAVEN_SETTINGS=~/.m2/settings.xml GPG_KEYID=... GPG_PASSPHRASE=...
	echo make publish-central GPG_PASSPHRASE=...

clean:
	$(MVN) -q clean

install:
	$(MVN) -q -U -DskipTests clean install

sign-install:
	$(MVN) -q -Psigning -DskipTests -D"gpg.keyname=$(GPG_KEYID)" -D"gpg.passphrase=$(GPG_PASSPHRASE)" clean install

deploy-local:
	$(MVN) -q -DskipTests -DaltDeploymentRepository=$(ALT_DEPLOY_REPO) deploy

deploy-central:
	$(MVN) -q $(SETTINGS_OPT) -Psigning,central-publish -DskipTests -Dmaven.deploy.skip=true -D"gpg.keyname=$(GPG_KEYID)" -D"gpg.passphrase=$(GPG_PASSPHRASE)" deploy

deploy-ossrh:
	$(MVN) -q $(SETTINGS_OPT) -Psigning,legacy-ossrh -DskipTests -D"gpg.keyname=$(GPG_KEYID)" -D"gpg.passphrase=$(GPG_PASSPHRASE)" deploy

publish-central:
	$(MVN) -s "./.mvn/settings.xml" -P signing,central-publish -DskipTests deploy
