pipeline {
    agent any

    environment {
        IMAGE_NAME = "tanmaysinghx/ts-auth-service-1625:latest"
        CONTAINER_NAME = "ts-auth-service-1625-dev"
        NGROK_AUTH = credentials('ngrok-auth-token')
        EXPOSED_PORT = "1625"
        INTERNAL_PORT = "8080"

        // 👇 Secure Secrets (stored in Jenkins credentials → Secret Text)
        ACCESS_TOKEN_SECRET = credentials('access-token-secret')
        REFRESH_TOKEN_SECRET = credentials('refresh-token-secret')
        DATABASE_URL_RAW = credentials('ts-auth-service-1625-db-url')
    }

    stages {
        stage('Pull Docker Image') {
            steps {
                sh "docker pull $IMAGE_NAME"
            }
        }

        stage('Run Container with Mounted CA Cert') {
    steps {
        withCredentials([file(credentialsId: 'ts-auth-service-1625-ca-cert', variable: 'CA_CERT_PATH')]) {
            script {
                def fullDbUrl = "${DATABASE_URL_RAW}&ssl-ca=/app/ca.pem"

                sh """
                    docker rm -f $CONTAINER_NAME || true
                    docker run -d --name $CONTAINER_NAME \
                      -p $EXPOSED_PORT:$INTERNAL_PORT \
                      -e ACCESS_TOKEN_SECRET=$ACCESS_TOKEN_SECRET \
                      -e REFRESH_TOKEN_SECRET=$REFRESH_TOKEN_SECRET \
                      -e DATABASE_URL='${fullDbUrl}' \
                      -v "$CA_CERT_PATH:/app/ca.pem" \
                      $IMAGE_NAME
                """
            }
        }
    }
}


        stage('Start Ngrok Tunnel') {
            steps {
                sh '''
                    pkill ngrok || true
                    ngrok authtoken $NGROK_AUTH
                    nohup ngrok http 1625 > ngrok.log &
                    sleep 5
                '''
            }
        }

        stage('Get Ngrok URL') {
            steps {
                script {
                    def url = sh(
                        script: "curl -s http://localhost:4040/api/tunnels | jq -r .tunnels[0].public_url",
                        returnStdout: true
                    ).trim()
                    echo "🌍 App Available via Ngrok: $url"
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                script {
                    sh 'curl -f http://localhost:1625/v2/api/health/health-check || exit 1'
                }
            }
        }
    }

    post {
        success {
            echo "✅ Dev Promotion Pipeline Completed Successfully!"
        }
        failure {
            echo "❌ Dev Promotion Failed!"
        }
    }
}
