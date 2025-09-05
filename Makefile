run-prodlike:
	docker compose -f docker-compose.prodlike.yaml up --build -d

stop-prodlike:
	docker compose -f docker-compose.prodlike.yaml down
