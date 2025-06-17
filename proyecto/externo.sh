#!/bin/bash

API_URL="http://localhost:8180/Service"
EMAIL="holahola2@test.es"
PASSWORD="holahola2"
NAME="holahola2"

echo "[*] Registrando usuario..."
signup_response=$(curl -s -X POST "$API_URL/signup" \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"$NAME\", \"email\": \"$EMAIL\", \"password\": \"$PASSWORD\"}")

if echo "$signup_response" | grep -q "Email already registered."; then
  echo "[*] Usuario ya registrado. Intentando login..."
  login_response=$(curl -s -X POST "$API_URL/checkLogin" \
    -H "Content-Type: application/json" \
    -d "{\"email\": \"$EMAIL\", \"password\": \"$PASSWORD\"}")
  PRIVATE_TOKEN=$(echo "$login_response" | jq -r '.token')
else
  PRIVATE_TOKEN=$(echo "$signup_response" | jq -r '.token')
fi

if [[ "$PRIVATE_TOKEN" == "null" || -z "$PRIVATE_TOKEN" ]]; then
  echo "[!] Error: No se pudo obtener el token."
  exit 1
fi

echo "[*] Token obtenido: $PRIVATE_TOKEN"

# Paso 1: Enviar petición con token incorrecto (opcional)
POST_URL="$API_URL/u/$EMAIL/dialogue"
NOW=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
WRONG_AUTH_TOKEN="1234567890abcdef"

echo "[*] Enviando petición con token incorrecto..."
curl -s -o /dev/null -w "Código HTTP: %{http_code}\n" -X POST "$POST_URL" \
  -H "Content-Type: application/json" \
  -H "User: $EMAIL" \
  -H "Date: $NOW" \
  -H "Auth-Token: $WRONG_AUTH_TOKEN" \
  -d '{"name": "test"}'

# Paso 2: Crear conversación con token correcto
AUTH_TOKEN=$(echo -n "$POST_URL$NOW$PRIVATE_TOKEN" | md5sum | awk '{print $1}')
dialogue_response=$(curl -i -s -X POST "$POST_URL" \
  -H "Content-Type: application/json" \
  -H "User: $EMAIL" \
  -H "Date: $NOW" \
  -H "Auth-Token: $AUTH_TOKEN" \
  -d '{"name": "test"}')

# Extraer cabecera Location (URL de la conversación)
LOCATION=$(echo "$dialogue_response" | grep -Fi Location | awk '{print $2}' | tr -d '\r\n')
if [[ -z "$LOCATION" ]]; then
  echo "[!] No se obtuvo URL de conversación. Abortando."
  exit 1
fi
echo "[*] Conversación creada: $LOCATION"

DIALOGUE_URL="$LOCATION"

# Paso 3: Obtener nextUrl desde GET a la conversación
NOW=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
GET_AUTH_TOKEN=$(echo -n "$DIALOGUE_URL$NOW$PRIVATE_TOKEN" | md5sum | awk '{print $1}')

dialogue_info=$(curl -s -X GET "$DIALOGUE_URL" \
  -H "User: $EMAIL" \
  -H "Date: $NOW" \
  -H "Auth-Token: $GET_AUTH_TOKEN")

NEXT_URL=$(echo "$dialogue_info" | jq -r '.nextUrl')
if [[ "$NEXT_URL" == "null" || -z "$NEXT_URL" ]]; then
  echo "[!] No se encontró nextUrl."
  exit 1
fi
FULL_NEXT_URL="$API_URL$NEXT_URL"
echo "[*] Próxima URL para enviar el prompt: $FULL_NEXT_URL"

# Paso 4: Enviar prompt
TIMESTAMP=$(date +%s%3N)
PROMPT='¿Cuál es la capital?'
PROMPT_PAYLOAD="{\"prompt\": \"$PROMPT\", \"timestamp\": $TIMESTAMP}"

NOW=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
PROMPT_AUTH_TOKEN=$(echo -n "$FULL_NEXT_URL$NOW$PRIVATE_TOKEN" | md5sum | awk '{print $1}')

echo "[*] Enviando prompt..."
prompt_response=$(curl -s -X POST "$FULL_NEXT_URL" \
  -H "Content-Type: application/json" \
  -H "User: $EMAIL" \
  -H "Date: $NOW" \
  -H "Auth-Token: $PROMPT_AUTH_TOKEN" \
  -d "$PROMPT_PAYLOAD")

echo "[*] Respuesta al enviar prompt:"
echo "$prompt_response"

# Paso 5: Polling hasta que el estado sea READY en DIALOGUE_URL (no en nextUrl)
STATUS=""
while [[ "$STATUS" != "READY" ]]; do
  sleep 1
  NOW=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  POLL_AUTH_TOKEN=$(echo -n "$DIALOGUE_URL$NOW$PRIVATE_TOKEN" | md5sum | awk '{print $1}')
  response=$(curl -s -X GET "$DIALOGUE_URL" \
    -H "User: $EMAIL" \
    -H "Date: $NOW" \
    -H "Auth-Token: $POLL_AUTH_TOKEN")

  echo "[*] Respuesta polling:"
  echo "$response"

  if echo "$response" | jq empty 2>/dev/null; then
    STATUS=$(echo "$response" | jq -r '.status')
    echo "[*] Estado: $STATUS"
  else
    echo "[!] Respuesta no es JSON válido. Abortando."
    exit 1
  fi
done

# Paso 6: Mostrar respuesta del prompt enviado
RESPONSE_TEXT=$(echo "$response" | jq -r ".dialogue[] | select(.prompt == \"$PROMPT\") | .response")
echo "[*] Respuesta recibida: $RESPONSE_TEXT"

# Paso 6: POST a endUrl
END_URL=$(echo "$response" | jq -r '.endUrl')
if [[ "$END_URL" != "null" && -n "$END_URL" ]]; then
  FULL_END_URL="$API_URL$END_URL"
  NOW=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  END_AUTH_TOKEN=$(echo -n "$FULL_END_URL$NOW$PRIVATE_TOKEN" | md5sum | awk '{print $1}')
  echo "[*] Enviando POST a endUrl..."
  curl -s -X POST        "$FULL_END_URL" \
    -H "User: $EMAIL" \
    -H "Date: $NOW" \
    -H "Auth-Token: $END_AUTH_TOKEN"
else
  echo "[!] endUrl no encontrado."
fi

# Paso 7: Obtener estadísticas
STATS_URL="$API_URL/u/$EMAIL/statistics"
NOW=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
STATS_AUTH_TOKEN=$(echo -n "$STATS_URL$NOW$PRIVATE_TOKEN" | md5sum | awk '{print $1}')
echo "[*] Estadísticas del usuario:"
curl -s -X GET "$STATS_URL" \
  -H "User: $EMAIL" \
  -H "Date: $NOW" \
  -H "Auth-Token: $STATS_AUTH_TOKEN" | jq

# Paso 8: Obtener logs
LOGS_URL="$API_URL/u/$EMAIL/dialogue/logs"
NOW=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
LOGS_AUTH_TOKEN=$(echo -n "$LOGS_URL$NOW$PRIVATE_TOKEN" | md5sum | awk '{print $1}')
logs_response=$(curl -s -X GET "$LOGS_URL" \
  -H "User: $EMAIL" \
  -H "Date: $NOW" \
  -H "Auth-Token: $LOGS_AUTH_TOKEN")

echo "[*] Logs de conversaciones:"
echo "$logs_response" | jq

# Paso 9: Eliminar conversación si existe
CONV_NAME=$(echo "$logs_response" | jq -r '.[0].name')
if [[ -n "$CONV_NAME" && "$CONV_NAME" != "null" ]]; then
  DELETE_LOG_URL="$API_URL/u/$EMAIL/dialogue/logs/$CONV_NAME"
  NOW=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  DEL_LOG_AUTH_TOKEN=$(echo -n "$DELETE_LOG_URL$NOW$PRIVATE_TOKEN" | md5sum | awk '{print $1}')
  echo "[*] Eliminando conversación '$CONV_NAME'..."
  delete_conv_response=$(curl -s -o /dev/null -w "Código HTTP: %{http_code}\n" -X DELETE "$DELETE_LOG_URL" \
    -H "User: $EMAIL" \
    -H "Date: $NOW" \
    -H "Auth-Token: $DEL_LOG_AUTH_TOKEN")
  echo "[*] Resultado borrar conversación: $delete_conv_response"
else
  echo "[!] No se encontró conversación para eliminar."
fi

# Paso 7: Borrar usuario con DELETE a /Service/u/email
DELETE_URL="$API_URL/u/$EMAIL"
NOW=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
DELETE_AUTH_TOKEN=$(echo -n "$DELETE_URL$NOW$PRIVATE_TOKEN" | md5sum | awk '{print $1}')

echo "[*] Borrando usuario $EMAIL..."
delete_response=$(curl -s -o /dev/null -w "Código HTTP: %{http_code}\n" -X DELETE "$DELETE_URL" \
  -H "User: $EMAIL" \
  -H "Date: $NOW" \
  -H "Auth-Token: $DELETE_AUTH_TOKEN")

echo "[*] Resultado borrar usuario: $delete_response"
