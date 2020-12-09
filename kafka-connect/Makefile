.PHONY: build-docker-image

IMAGE_TAG := 0.0.2
NAMESPACE := kafka

build-docker-image:
	docker build -t pinkstack/kafka-connect \
		-t pinkstack/kafka-connect:latest \
		-t pinkstack/kafka-connect:${IMAGE_TAG} \
		-f ./../.docker/kafka-connect.Dockerfile .

push-docker-image: build-docker-image
	docker push pinkstack/kafka-connect:${IMAGE_TAG}

create-ns:
	kubectl create namespace ${NAMESPACE} || true
	kubens ${NAMESPACE}

echo-tag:
	echo ${IMAGE_TAG}