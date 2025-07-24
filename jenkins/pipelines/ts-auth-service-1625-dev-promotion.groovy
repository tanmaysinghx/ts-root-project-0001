pipeline {
    agent any

    environment {
        IMAGE_NAME = "tanmaysinghx/ts-auth-service-1625:latest"
        CONTAINER_NAME = "ts-auth-service-1625-dev"
        NGROK_AUTH = credentials('ngrok-auth-token')
        EXPOSED_PORT = "1625"
        INTERNAL_PORT = "1625"

        // Add your env vars here if you want
        // Or you can inject them externally from Jenkins global env or parameters
        DATABASE_URL = credentials('db-url') // or plain env var if configured differently
        ACCESS_TOKEN_SECRET = credentials('access-token-secret')
        REFRESH_TOKEN_SECRET = credentials('refresh-token-secret')
        EMAIL_USER = credentials('email-user')
        EMAIL_PASS = credentials('email-pass')
    }

    stages {
        stage('Pull Docker Image') {
            steps {
                sh "docker pull $IMAGE_NAME"
            }
        }

        stage('Write .env File From Jenkins Environment Variables') {
            steps {
                // Compose the .env file from Jenkins env vars
                sh """
                    echo "DATABASE_URL=$DATABASE_URL" > .env
                    echo "ACCESS_TOKEN_SECRET=$ACCESS_TOKEN_SECRET" >> .env
                    echo "REFRESH_TOKEN_SECRET=$REFRESH_TOKEN_SECRET" >> .env
                    echo "EMAIL_USER=$EMAIL_USER" >> .env
                    echo "EMAIL_PASS=$EMAIL_PASS" >> .env
                """
                // Optional debug - uncomment if troubleshooting
                // sh 'cat .env'
            }
        }

        stage('Run Docker Container with --env-file') {
            steps {
                sh """
                    docker rm -f $CONTAINER_NAME || true

                    docker run -d --name $CONTAINER_NAME \\
                        --env-file .env \\
                        -p $EXPOSED_PORT:$INTERNAL_PORT \\
                        $IMAGE_NAME
                """
            }
        }

        stage('Start Ngrok Tunnel') {
            steps {
                sh """
                    pkill ngrok || true
                    ngrok authtoken $NGROK_AUTH
                    nohup ngrok http $EXPOSED_PORT > ngrok.log 2>&1 &
                    sleep 5
                """
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
                sh "curl -f http://localhost:$EXPOSED_PORT/v2/api/health/health-check || exit 1"
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
        always {
            sh 'rm -f .env || true'
        }
    }
}
