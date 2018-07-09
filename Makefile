.DEFAULT_GOAL := build

IMAGE ?= docker.io/agilestacks/jenkins-operator
TAG ?= $(shell git rev-parse HEAD | colrm 7)
NAMESPACE ?= jenkins

kubectl := kubectl

build:
	docker build -t $(IMAGE):latest -t $(IMAGE):$(TAG) .
.PHONY: build

push:
	docker push $(IMAGE):latest
	docker push $(IMAGE):$(TAG)
.PHONY: push

push-experimental:
	docker tag $(IMAGE):$(TAG) $(IMAGE):experimental
	docker push $(IMAGE):experimental
.PHONY: push-experimental

deploy: make push

.PHONY: deploy
	- $(kubectl) create namespace $(NAMESPACE)
undeploy:

.PHONY: undeploy
