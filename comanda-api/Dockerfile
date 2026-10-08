# Receita para o Render montar e rodar o back (o plano gratuito do Render só roda Java via Docker).

# Etapa 1: compila o projeto com Maven e gera o .jar
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Etapa 2: imagem final, só com o Java e o .jar (fica bem menor)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# -Xmx256m / SerialGC: o plano gratuito tem só 512 MB de memória
# user.timezone: sem isso as datas dos pedidos sairiam no horário de Londres (UTC), 3 horas adiantadas
ENTRYPOINT ["java", "-Xmx256m", "-XX:+UseSerialGC", "-Duser.timezone=America/Sao_Paulo", "-jar", "app.jar"]
