#!/bin/sh
# MetajavBots en Linux y Mac: ./jugar.sh (o ./jugar.sh prueba, ./jugar.sh capturas)
cd "$(dirname "$0")"
case "$1" in
  prueba) exec ./gradlew -q pruebas:prueba ;;
  capturas) exec ./gradlew -q lwjgl3:capturas ;;
  *) exec ./gradlew -q lwjgl3:run ;;
esac
