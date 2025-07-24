pipeline {
    agent any

    environment {
        IMAGE_NAME = "tanmaysinghx/ts-auth-service-1625:latest"
        CONTAINER_NAME = "ts-auth-service-1625-dev"
        NGROK_AUTH = credentials('ngrok-auth-token')
        EXPOSED_PORT = "1625"
        INTERNAL_PORT = "1625"
    }

    stages {
        stage('Pull Docker Image') {
            steps {
                sh "docker pull $IMAGE_NAME"
            }
        }

        stage('Write Env File & Run Container') {
            steps {
                sh '''
                    cp /var/jenkins_home/envs/ts-auth-service-1625.env .env

                    docker rm -f $CONTAINER_NAME || true

                    docker run -d --name $CONTAINER_NAME \
                      --env-file .env \
                      -p $EXPOSED_PORT:$INTERNAL_PORT \
                      $IMAGE_NAME
                '''
            }
        }

        stage('Start Ngrok Tunnel') {
            steps {
                sh '''
                    pkill ngrok || true
                    ngrok authtoken $NGROK_AUTH
                    nohup ngrok http $EXPOSED_PORT > ngrok.log 2>&1 &
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
