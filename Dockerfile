# Imagen para desplegar UFound (Render, Railway, Koyeb, Fly.io...).
# Se construye desde la raiz del repo porque el pom.xml empaqueta ../frontend.

# --- Etapa 1: compilar el .jar ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Primero solo el pom para cachear las dependencias entre builds
COPY backend/pom.xml backend/pom.xml
RUN mvn -f backend/pom.xml -B -q dependency:go-offline

COPY backend/src backend/src
COPY frontend frontend
RUN mvn -f backend/pom.xml -B -DskipTests package

# --- Etapa 2: imagen liviana solo con el JRE ---
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/backend/target/*.jar app.jar

# Los planes gratuitos suelen dar 512 MB de RAM: limitamos el heap para no pasarnos.
ENV JAVA_OPTS="-Xmx300m -XX:+UseSerialGC -Xss512k"
EXPOSE 8080

# La plataforma inyecta PORT; application.properties lo lee con server.port=${PORT:8080}
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
