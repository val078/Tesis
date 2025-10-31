pipeline {
    agent {
        docker {
            image 'openjdk:17-slim'
            args '-u root'
        }
    }
    
    options {
        timeout(time: 60, unit: 'MINUTES')
        timestamps()
    }
    
    environment {
        DOCKER_IMAGE = 'android-app-tesis'
        DOCKER_TAG = "${BUILD_NUMBER}"
        FIREBASE_TOKEN = credentials('firebase-token')
        FIREBASE_APP_ID = '1:445628311030:android:b4e1e2eb06ea93b80593d7'
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo 'Obteniendo código desde GitHub...'
                checkout scm
                sh 'chmod +x gradlew'
            }
        }
        
        stage('Build APK Gradle') {
            options {
                timeout(time: 40, unit: 'MINUTES')
            }
            steps {
                echo 'Instalando Android SDK y compilando el APK...'
                sh '''
                    set -e  #Detiene el script si ocurre algún error
                    
                    echo "Instalando dependencias básicas..."
                    apt-get update -y && apt-get install -y wget unzip > /dev/null

                    export ANDROID_SDK_ROOT=$WORKSPACE/android-sdk
                    mkdir -p $ANDROID_SDK_ROOT/cmdline-tools
                    cd $ANDROID_SDK_ROOT/cmdline-tools

                    echo "Descargando Android Command Line Tools..."
                    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O tools.zip
                    unzip -q tools.zip
                    mkdir -p $ANDROID_SDK_ROOT/cmdline-tools/latest
                    mv cmdline-tools/* $ANDROID_SDK_ROOT/cmdline-tools/latest/ || true  # evita error si carpeta ya existe

                    echo "Aceptando licencias e instalando plataformas..."
                    yes | $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --licenses > /dev/null
                    $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager \
                        "platform-tools" "platforms;android-35" "build-tools;35.0.0"

                    echo "sdk.dir=$ANDROID_SDK_ROOT" > $WORKSPACE/local.properties

                    echo "Compilando con Gradle..."
                    cd $WORKSPACE
                    ./gradlew clean assembleDebug --no-daemon --stacktrace --console=plain
                '''
            }
        }
        
        stage('Ejecutar Tests') {
            steps {
                echo 'Ejecutando pruebas unitarias...'
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    sh './gradlew test --no-daemon'
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                echo 'Construyendo imagen Docker...'
                script {
                    docker.build("${DOCKER_IMAGE}:${DOCKER_TAG}")
                    docker.build("${DOCKER_IMAGE}:latest")
                }
            }
        }
        
        stage('Subir a Firebase App Distribution') {
            when {
                expression { env.FIREBASE_APP_ID != '1:445628311030:android:b4e1e2eb06ea93b80593d7' }
            }
            steps {
                echo 'Subiendo APK a Firebase para distribución por QR...'
                sh '''
                    npm list -g firebase-tools || npm install -g firebase-tools
                    
                    firebase appdistribution:distribute \
                        app/build/outputs/apk/debug/app-debug.apk \
                        --app ${FIREBASE_APP_ID} \
                        --token ${FIREBASE_TOKEN} \
                        --groups "testers" \
                        --release-notes "Build #${BUILD_NUMBER} - Compilado automáticamente via CI/CD"
                '''
            }
        }
        
        stage('Archivar APK') {
            steps {
                echo 'Guardando APK como artefacto...'
                archiveArtifacts artifacts: '**/build/outputs/apk/debug/*.apk', fingerprint: true, allowEmptyArchive: true
            }
        }
    }
    
    post {
        success {
            echo '¡Pipeline completado exitosamente!'
            echo "Imagen Docker creada: ${DOCKER_IMAGE}:${DOCKER_TAG}"
            echo 'APK disponible en artifacts'
        }
        failure {
            echo 'El build falló. Revisa los logs.'
        }
        always {
            echo 'Limpiando workspace...'
            cleanWs()
        }
    }
}
