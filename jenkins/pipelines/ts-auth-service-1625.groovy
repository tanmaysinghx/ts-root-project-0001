pipeline {
    agent any

    environment {
        SERVICE_NAME = 'ts-auth-service-1625'
        SERVICE_REPO = 'https://github.com/your-org/ts-auth-service-1625.git'
        SERVICE_PORT = '1625'
    }

    stages {
        stage('Clone Microservice Repo') {
            steps {
                dir("backend/${env.SERVICE_NAME}") {
                    git url: "${env.SERVICE_REPO}", branch: 'main'
                }
            }
        }

        stage('Build with Docker Compose') {
            steps {
                sh "docker compose --env-file .env -f docker-compose.yml build ${env.SERVICE_NAME}"
            }
        }

        stage('Start Service Container') {
            steps {
                sh "docker compose --env-file .env -f docker-compose.yml up -d ${env.SERVICE_NAME}"
            }
        }

        stage('Expose via Ngrok') {
            steps {
                sh """
                docker rm -f ngrok-${env.SERVICE_NAME} || true
                docker run -d --name ngrok-${env.SERVICE_NAME} --net=host wernight/ngrok ngrok http ${env.SERVICE_PORT}
                """
            }
        }
    }

    post {
        success {
            echo "✅ Service ${env.SERVICE_NAME} deployed and exposed."
        }
        failure {
            echo "❌ Deployment failed for ${env.SERVICE_NAME}."
        }
    }
}
