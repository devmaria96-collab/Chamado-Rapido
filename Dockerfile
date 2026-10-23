FROM node:22-alpine AS frontend
WORKDIR /build/frontend
COPY frontend/package*.json ./
RUN if [ -f package-lock.json ]; then npm ci --no-audit --no-fund; else npm install --no-audit --no-fund; fi
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /build/backend
COPY backend/pom.xml ./
COPY backend/src ./src
COPY --from=frontend /build/frontend/dist/chamado-rapido/browser ./src/main/resources/static
# Compilacao e testes obrigatorios antes de gerar a imagem.
RUN mvn -B clean verify

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S -g 10001 chamado && adduser -S -D -u 10001 -G chamado chamado
WORKDIR /app
RUN mkdir -p /app/storage && chown -R chamado:chamado /app
COPY --from=backend --chown=chamado:chamado /build/backend/target/chamado-rapido.jar ./app.jar
USER chamado
ENV SPRING_PROFILES_ACTIVE=container UPLOAD_DIR=/app/storage
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 CMD wget -q -O /dev/null http://127.0.0.1:8080/api/public/info || exit 1
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70.0","-jar","/app/app.jar"]
