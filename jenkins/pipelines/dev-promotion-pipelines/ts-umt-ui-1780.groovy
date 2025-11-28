pipeline {
    agent any

    parameters {
        // Allows you to deploy 'latest' or roll back to a specific tag like '4'
        string(name: 'TAG_TO_DEPLOY', defaultValue: 'latest', description: 'Enter the Image Tag to deploy (e.g., latest, 4, 5)')
    }

    environment {
        // --- CONFIGURATION FOR ZERO-ANGW ---
        DOCKER_IMAGE_NAME = 'tanmaysinghx/ts-umt-ui-1780'
        CONTAINER_NAME    = 'tanmaysinghx/ts-umt-ui-1780'
        PORT              = '1780'
        
        // Ngrok Config (Uncomment if needed)
        // NGROK_AUTH     = credentials('ngrok-auth-token') 
    }

    stages {
        stage('Pull Image') {
            steps {
                script {
                    echo "Pulling image: ${DOCKER_IMAGE_NAME}:${params.TAG_TO_DEPLOY}"
                    // REMOVED 'sudo'. Ensure jenkins user is in docker group.
                    sh "docker pull ${DOCKER_IMAGE_NAME}:${params.TAG_TO_DEPLOY}"
                }
            }
        }

        stage('Deploy Container') {
            steps {
                script {
                    sh """
                        echo "--- Stopping old container ---"
                        docker rm -f ${CONTAINER_NAME} || true
                        
                        echo "--- Starting new container on port ${PORT} ---"
                        docker run -d \\
                            -p ${PORT}:${PORT} \\
                            --name ${CONTAINER_NAME} \\
                            --restart always \\
                            ${DOCKER_IMAGE_NAME}:${params.TAG_TO_DEPLOY}
                    """
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                script {
                    echo "Waiting for container to initialize..."
                    sleep 5 
                    
                    // Check if container is running
                    sh "docker ps | grep ${CONTAINER_NAME}"
                    
                    // Check if app is serving content
                    sh "curl -f http://localhost:${PORT}/ || exit 1"
                    echo "✅ App is healthy and accessible on port ${PORT}"
                }
            }
        }
        
        /* stage('Start Ngrok Tunnel') {
            steps {
                sh '''
                    pkill ngrok || true
                    ngrok authtoken $NGROK_AUTH
                    nohup ngrok http $PORT > ngrok.log &
                    sleep 5
                '''
                script {
                    def url = sh(script: "curl -s http://localhost:4040/api/tunnels | jq -r .tunnels[0].public_url", returnStdout: true).trim()
                    echo "🌍 Public URL: $url"
                }
            }
        }
        */
    }

    post {
        success {
            echo "✅ Deployment of ${DOCKER_IMAGE_NAME}:${params.TAG_TO_DEPLOY} Successful!"
        }
        failure {
            echo "❌ Deployment Failed."
        }
    }
}