pipeline {
    agent any
    
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
        
        stage('Build APK con Gradle') {
            steps {
                echo '🔨 Compilando APK con Gradle...'
                sh './gradlew clean assembleDebug --no-daemon'
            }
        }
        
        stage('Ejecutar Tests') {
            steps {
                echo 'Ejecutando pruebas unitarias...'
                sh './gradlew test --no-daemon'
            }
        }
        
        stage('Build Docker Image') {
            steps {
                echo '🐳 Construyendo imagen Docker...'
                script {
                    docker.build("${DOCKER_IMAGE}:${DOCKER_TAG}")
                    docker.build("${DOCKER_IMAGE}:latest")
                }
            }
        }
        
        stage('Firebase App Distribution') {
            steps {
                echo 'Subiendo APK a Firebase para distribución por QR...'
                sh '''
                    # Instalar Firebase CLI si no está instalado
                    npm list -g firebase-tools || npm install -g firebase-tools
                    
                    # Distribuir APK a Firebase
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
                archiveArtifacts artifacts: '**/build/outputs/apk/debug/*.apk', fingerprint: true
            }
        }
    }
    
    post {
        success {
            echo 'Pipeline completado exitosamente'
            echo "Imagen Docker creada: ${DOCKER_IMAGE}:${DOCKER_TAG}"
            echo 'APK disponible en Firebase App Distribution'
            echo 'Genera el QR desde Firebase Console para distribuir'
        }
        failure {
            echo 'Build failed.'
        }
        always {
            echo 'Limpiando workspace...'
            cleanWs()
        }
    }
}
