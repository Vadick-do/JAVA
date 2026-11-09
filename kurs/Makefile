.PHONY: build test run up down logs

build:
	docker compose build app

test:
	docker compose run --rm -T --build test

run:
	docker compose run --rm -T --build app

up:
	docker compose up -d --wait postgres

down:
	docker compose down

logs:
	docker compose logs postgres
