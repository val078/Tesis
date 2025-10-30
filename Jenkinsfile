pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = 'android-app-tesis'
        DOCKER_TAG = "${BUILD_NUMBER}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo 'Obteniendo código desde GitHub...'
                checkout scm
            }
        }
        
        stage('Build with Gradle') {
            steps {
                echo 'Compilando aplicación Android...'
                sh './gradlew clean assembleDebug'
            }
        }
        
        stage('Test') {
            steps {
                echo 'Ejecutando pruebas unitarias...'
                sh './gradlew test'
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
        
        stage('Archive APK') {
            steps {
                echo 'Guardando APK generado...'
                archiveArtifacts artifacts: '**/build/outputs/apk/debug/*.apk', fingerprint: true
            }
        }
    }
    
    post {
        success {
            echo '¡Build exitoso!'
            echo "Imagen Docker creada: ${DOCKER_IMAGE}:${DOCKER_TAG}"
        }
        failure {
            echo 'Build falló'
        }
        always {
            echo 'Limpiando workspace...'
            cleanWs()
        }
    }
}
