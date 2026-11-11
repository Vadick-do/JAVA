.PHONY: build test test-integration run up down logs

build:
	docker compose build app

test:
	docker compose run --rm -T --build test

test-integration:
	@set -eu; \
	project=booking-coursework-it-$$$$; \
	export DB_NAME=booking_coursework_test DB_USER=booking DB_PASSWORD=integration-only; \
	compose() { docker compose --env-file /dev/null --profile test -p "$$project" "$$@"; }; \
	trap 'compose down --volumes --rmi local' EXIT; \
	for source in src/test/java/ru/coursework/booking/integration/*IT.java; do \
		suite=$${source##*/}; \
		suite=$${suite%.java}; \
		compose up -d --wait postgres; \
		compose run --rm -T --build -e DB_NAME -e DB_USER -e DB_PASSWORD test \
			mvn -B -ntp -o -Dtest="$$suite" test; \
		compose down --volumes --rmi local; \
	done

run:
	docker compose run --rm -T --build app

up:
	docker compose up -d --wait postgres socat

down:
	docker compose down

logs:
	docker compose logs postgres socat
