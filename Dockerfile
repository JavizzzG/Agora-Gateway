# ─────────────────────────────────────────
# ETAPA 1: construcción
# Usamos una imagen con Maven y JDK para compilar
# ─────────────────────────────────────────
FROM --platform=$TARGETPLATFORM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiamos primero solo el pom.xml y descargamos dependencias
# Esto es un truco de caché: si el pom.xml no cambia, Docker
# reutiliza esta capa y no re-descarga las dependencias cada vez
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Ahora copiamos el código fuente y compilamos
COPY src ./src
RUN mvn clean package -DskipTests -q

# ─────────────────────────────────────────
# ETAPA 2: imagen final
# Solo JRE (no JDK completo) — imagen más pequeña y segura
# ─────────────────────────────────────────
FROM --platform=$TARGETPLATFORM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiamos solo el JAR desde la etapa de construcción
COPY --from=builder /app/target/*.jar gateway.jar

# Puerto que expone el gateway
EXPOSE 8080

# Comando para arrancar
ENTRYPOINT ["java", "-jar", "gateway.jar"]