#!/bin/bash

STACK_NAME="ssdd"

echo "=== Paso 1: Iniciando Docker Swarm (si es necesario) ==="
docker info | grep -q "Swarm: active"
if [ $? -ne 0 ]; then
  docker swarm init
fi

echo "=== Paso 2: Construyendo imágenes necesarias ==="
docker build -t ssdd-db-mysql ./db-mysql
docker build -t ssdd-backend-rest ./backend-rest/es.um.sisdist.backend.Service
docker build -t ssdd-backend-rest-externo ./backend-rest-externo/es.um.sisdist.backend.Service
docker build -t ssdd-backend-grpc ./backend-grpc/es.um.sisdist.backend.grpc.GrpcServiceImpl
docker build -t ssdd-frontend -f frontend/Dockerfile-devel ./frontend

echo "=== Paso 3: Desplegando stack '${STACK_NAME}' ==="
docker stack deploy -c docker-stack.yml $STACK_NAME

echo "=== Paso 4: Mostrando estado de los servicios ==="
docker stack services $STACK_NAME

echo "=== Despliegue completo ==="
echo "Accede al frontend en: http://localhost:5010/"