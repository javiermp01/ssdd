#!/bin/bash
# Script para compilar todos los módulos Maven (con pom.xml) recursivamente

find . -name "pom.xml" -execdir mvn clean install -DskipTests \;
