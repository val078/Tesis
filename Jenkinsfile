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
                echo 'Instalando Android SDK y compilando APK...'
                withCredentials([file(credentialsId: 'firebase-google-services-json', variable: 'GOOGLE_SERVICES_FILE')]) {
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

                        echo "Copiando google-services.json..."
                        cp $GOOGLE_SERVICES_FILE $WORKSPACE/app/google-services.json
                        echo "Archivo copiado en: app/google-services.json"
        
                        echo "Compilando APK con Gradle..."
                        cd $WORKSPACE
                        ./gradlew clean assembleDebug --no-daemon --console=plain
                    '''
                }
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
        
        stage('Subir a Firebase App Distribution') {
            steps {
                echo 'Subiendo APK a Firebase para distribución por QR...'
                sh '''
                    set -e
                    
                    # Instalar curl primero
                    echo "Instalando dependencias..."
                    apt-get update -qq > /dev/null 2>&1
                    apt-get install -y curl > /dev/null 2>&1
                    
                    # Instalar Node.js y npm
                    echo "Instalando Node.js..."
                    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - > /dev/null 2>&1
                    apt-get install -y nodejs > /dev/null 2>&1
                    
                    # Verificar instalación
                    echo "Node.js version: $(node --version)"
                    echo "npm version: $(npm --version)"
                    
                    # Instalar Firebase CLI
                    echo "Instalando Firebase CLI..."
                    npm install -g firebase-tools
                    
                    # Distribuir APK
                    echo "Distribuyendo APK a Firebase..."
                    firebase appdistribution:distribute \
                        app/build/outputs/apk/debug/app-debug.apk \
                        --app ${FIREBASE_APP_ID} \
                        --token ${FIREBASE_TOKEN} \
                        --groups "testers" \
                        --release-notes "Build #${BUILD_NUMBER} - Compilado automáticamente vía CI/CD"
                    
                    echo "APK subido exitosamente a Firebase App Distribution"
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
            echo 'APK generado y subido a Firebase'
            echo 'Ve a Firebase Console para obtener el link de descarga y generar el QR'
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
