# Stage 1: build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
# Las pruebas SI corren aqui. Iban con -DskipTests desde que src/test estaba
# vacio, o sea que saltarselas no costaba nada; ahora que hay pruebas, dejarlo
# asi significaria que el despliegue no verifica nada. Si una falla, la imagen
# no se construye y la version rota nunca llega a Fly.
RUN mvn package -q

# Stage 2: runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Duser.timezone=America/Mexico_City", \
  "-jar", "app.jar"]
