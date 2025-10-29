# Imagen base con JDK y Android SDK
FROM openjdk:11-jdk-slim

# Variables de entorno
ENV ANDROID_SDK_ROOT /opt/android-sdk
ENV ANDROID_HOME /opt/android-sdk
ENV PATH ${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools

# Instalar dependencias
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# Descargar e instalar Android SDK
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools && \
    mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Aceptar licencias y instalar herramientas necesarias
RUN yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" "platforms;android-33" "build-tools;33.0.0"

# Directorio de trabajo
WORKDIR /app

# Copiar archivos del proyecto
COPY . .

# Dar permisos a gradlew
RUN chmod +x ./gradlew

# Compilar la aplicación
RUN ./gradlew assembleDebug

# Exponer el APK generado
CMD ["cp", "-r", "app/build/outputs/apk/", "/output/"]
