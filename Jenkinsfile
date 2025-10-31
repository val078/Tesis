pipeline {
    agent any
    
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
                echo 'Compilando APK con Gradle...'
                sh './gradlew clean assembleDebug --no-daemon --stacktrace'
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
