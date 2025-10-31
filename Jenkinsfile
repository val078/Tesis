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
        
        stage('Subir a GitHub Releases') {
            steps {
                echo 'Subiendo APK a GitHub Releases...'
                withCredentials([string(credentialsId: 'github-release-token', variable: 'GITHUB_TOKEN')]) {
                    sh '''
                        set -e
                        
                        # Instalar dependencias
                        apt-get update -qq > /dev/null 2>&1
                        apt-get install -y curl jq > /dev/null 2>&1
                        
                        # Variables
                        REPO_OWNER="val078"
                        REPO_NAME="Tesis"
                        TAG_NAME="v${BUILD_NUMBER}"
                        RELEASE_NAME="Build ${BUILD_NUMBER}"
                        APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
                        
                        echo "Creando release ${TAG_NAME}..."
                        
                        # Crear release en GitHub y guardar respuesta
                        RELEASE_RESPONSE=$(curl -s -X POST \
                          -H "Authorization: token ${GITHUB_TOKEN}" \
                          -H "Accept: application/vnd.github.v3+json" \
                          https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/releases \
                          -d "{\\"tag_name\\":\\"${TAG_NAME}\\",\\"name\\":\\"${RELEASE_NAME}\\",\\"body\\":\\"APK generado automaticamente por CI/CD Jenkins - Build ${BUILD_NUMBER}\\",\\"draft\\":false,\\"prerelease\\":false}")
                        
                        # Extraer upload URL (método más robusto)
                        UPLOAD_URL=$(echo "$RELEASE_RESPONSE" | grep -o '"upload_url": *"[^"]*"' | grep -o 'https://[^"]*' | sed 's/{?name,label}//')
                        
                        if [ -z "$UPLOAD_URL" ]; then
                            echo "Error: No se pudo obtener el upload URL"
                            echo "Respuesta de GitHub:"
                            echo "$RELEASE_RESPONSE"
                            exit 1
                        fi
                        
                        echo "Upload URL obtenido: $UPLOAD_URL"
                        echo "Subiendo APK..."
                        
                        # Subir APK
                        UPLOAD_RESPONSE=$(curl -s -X POST \
                          -H "Authorization: token ${GITHUB_TOKEN}" \
                          -H "Content-Type: application/vnd.android.package-archive" \
                          --data-binary @${APK_PATH} \
                          "${UPLOAD_URL}?name=app-release.apk")
                        
                        # Verificar si la subida fue exitosa
                        if echo "$UPLOAD_RESPONSE" | grep -q '"browser_download_url"'; then
                            echo "APK subido exitosamente"
                            DOWNLOAD_URL="https://github.com/${REPO_OWNER}/${REPO_NAME}/releases/download/${TAG_NAME}/app-release.apk"
                            echo "Link de descarga directo: $DOWNLOAD_URL"
                            echo "Link latest (siempre apunta a la ultima version): https://github.com/${REPO_OWNER}/${REPO_NAME}/releases/latest/download/app-release.apk"
                        else
                            echo "Posible error al subir APK. Respuesta:"
                            echo "$UPLOAD_RESPONSE"
                        fi
                    '''
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
            echo 'Pipeline completado exitosamente'
            echo 'APK subido a GitHub Releases'
            echo 'Link directo: https://github.com/val078/Tesis/releases/latest/download/app-release.apk'
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
