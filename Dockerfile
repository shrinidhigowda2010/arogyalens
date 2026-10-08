# ---- Build frontend ----
FROM node:22-alpine AS web
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
# Same-origin API calls (/api/...) in production
ENV VITE_API_URL=""
RUN npm run build

# ---- Build backend (serves the built frontend as static files) ----
FROM maven:3.9-eclipse-temurin-21 AS api
WORKDIR /app/backend
COPY backend/pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY backend/src ./src
COPY --from=web /app/frontend/dist ./src/main/resources/static
RUN mvn -q -B -DskipTests package && cp target/*.jar /app/app.jar

# ---- Runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=api /app/app.jar ./app.jar
ENV AI_ENABLED=true \
    GEMINI_MODEL=gemini-flash-latest \
    DEMO_ENABLED=false \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
# Cloud Run provides $PORT (defaults to 8080)
CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
