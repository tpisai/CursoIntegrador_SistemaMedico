# Etapa 1: compila el jar con el mismo Maven Wrapper que se usa en local.
FROM eclipse-temurin:25-jdk AS compilacion

# La imagen no trae curl ni unzip, que mvnw usa para descargar Maven.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Las dependencias van en su propia capa: solo se descargan de nuevo si cambia pom.xml.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q -DskipTests package


# Etapa 2: imagen final, solo con el JRE y el jar.
FROM eclipse-temurin:25-jre

# curl es para el healthcheck: esta imagen tampoco lo trae. El UID es fijo para que el usuario siga
# siendo dueño del volumen de archivos aunque la imagen base cambie en una actualización.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --no-create-home saludgrau

WORKDIR /app
COPY --from=compilacion /app/target/*.jar app.jar

# Los documentos médicos se guardan en ./archivos (saludgrau.archivos.carpeta). La carpeta se crea
# aquí con su dueño para que el volumen, al estar vacío, herede esos permisos.
RUN mkdir archivos && chown saludgrau:saludgrau archivos

USER saludgrau

ENV SPRING_PROFILES_ACTIVE=prod
# Sin esto el contenedor usa UTC y "hoy" cambia a las 7 p. m. de Lima (citas, recordatorios).
ENV TZ=America/Lima

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
