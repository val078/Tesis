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
        
        stage('Build APK con Gradle') {
            options {
                timeout(time: 40, unit: 'MINUTES')
            }
            steps {
                echo '🔨 Instalando Android SDK y compilando APK...'
                sh '''
                    set -e
                    
                    echo "Instalando dependencias básicas..."
                    apt-get update -qq && apt-get install -y wget unzip > /dev/null
    
                    export ANDROID_SDK_ROOT=$WORKSPACE/android-sdk
                    mkdir -p $ANDROID_SDK_ROOT/cmdline-tools
                    cd $ANDROID_SDK_ROOT/cmdline-tools
    
                    echo "Descargando Android Command Line Tools..."
                    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O tools.zip
                    unzip -q tools.zip
                    mkdir -p $ANDROID_SDK_ROOT/cmdline-tools/latest
                    mv cmdline-tools/* $ANDROID_SDK_ROOT/cmdline-tools/latest/ 2>/dev/null || true
    
                    echo "Aceptando licencias e instalando plataformas..."
                    yes | $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --licenses > /dev/null 2>&1
                    $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager \
                        "platform-tools" "platforms;android-35" "build-tools;35.0.0" > /dev/null
    
                    echo "sdk.dir=$ANDROID_SDK_ROOT" > $WORKSPACE/local.properties
    
                    echo "🔨 Compilando APK con Gradle..."
                    cd $WORKSPACE
                    ./gradlew clean assembleDebug --no-daemon --console=plain
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
            echo 'APK generado correctamente'
            echo 'Descarga el APK desde Jenkins Artifacts'
        }
        failure {
            echo 'El build falló. Revisa los logs arriba.'
        }
        always {
            echo 'Limpiando workspace...'
            cleanWs()
        }
    }
}
