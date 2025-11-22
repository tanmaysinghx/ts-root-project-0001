pipeline {
    agent any

    parameters {
        // This allows you to type a specific tag (e.g., "15") or leave as "latest"
        string(name: 'TAG_TO_DEPLOY', defaultValue: 'latest', description: 'Enter the Image Tag to deploy (e.g., latest, 5, 6)')
    }

    environment {
        DOCKER_IMAGE_NAME = 'tanmaysinghx/ts-api-engine-service-1606'
        CONTAINER_NAME    = 'api-engine-1606'
        HOST_PORT         = '1606'
        CONTAINER_PORT    = '1606'
    }

    stages {
        stage('Pull Latest Image') {
            steps {
                script {
                    echo "Pulling image: ${DOCKER_IMAGE_NAME}:${params.TAG_TO_DEPLOY}"
                    // We pull explicitly to ensure we have the newest version from Hub
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
                        
                        echo "--- Starting new container ---"
                        docker run -d \\
                            -p ${HOST_PORT}:${CONTAINER_PORT} \\
                            --name ${CONTAINER_NAME} \\
                            ${DOCKER_IMAGE_NAME}:${params.TAG_TO_DEPLOY}
                    """
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                script {
                    // Quick check to see if it stayed running
                    sh "sleep 5" 
                    sh "docker ps | grep ${CONTAINER_NAME}"
                }
            }
        }
    }
}