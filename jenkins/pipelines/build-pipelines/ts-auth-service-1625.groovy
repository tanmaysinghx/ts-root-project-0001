pipeline {
    agent any

    environment {
        // Database Credentials
        TS_1625_DATABASE_URL         = credentials('TS_1625_DATABASE_URL')
        TS_1625_ACCESS_TOKEN_SECRET  = credentials('TS_1625_ACCESS_TOKEN_SECRET')
        TS_1625_REFRESH_TOKEN_SECRET = credentials('TS_1625_REFRESH_TOKEN_SECRET')
        
        // Docker Config
        DOCKER_IMAGE_NAME            = 'tanmaysinghx/ts-auth-service-1625'
        // Uses the Jenkins Build Number (e.g., 42) to create a unique tag
        IMAGE_TAG                    = "${BUILD_NUMBER}" 
    }

    stages {
        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Clone Service Repo') {
            steps {
                dir('ts-auth-service-1625') {
                    git branch: 'main',
                        url: 'https://github.com/tanmaysinghx/ts-auth-service-1625.git',
                        credentialsId: 'github-token'
                }
            }
        }

        stage('Write .env file') {
            steps {
                dir('ts-auth-service-1625') {
                    script {
                        def envContent = """
DATABASE_URL="${TS_1625_DATABASE_URL}"
ACCESS_TOKEN_SECRET="${TS_1625_ACCESS_TOKEN_SECRET}"
REFRESH_TOKEN_SECRET="${TS_1625_REFRESH_TOKEN_SECRET}"
PORT=1625
API_VERSION=v2
"""
                        // Hiding the output from logs for security
                        writeFile file: '.env', text: envContent.trim()
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                dir('ts-auth-service-1625') {
                    // Build with TWO tags: one specific version, and one 'latest'
                    sh "docker build -t ${DOCKER_IMAGE_NAME}:${IMAGE_TAG} -t ${DOCKER_IMAGE_NAME}:latest ."
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKERHUB_USER',
                    passwordVariable: 'DOCKERHUB_PASS'
                )]) {
                    sh """
                        echo "$DOCKERHUB_PASS" | docker login -u "$DOCKERHUB_USER" --password-stdin
                        
                        # Push specific version (Safe)
                        docker push ${DOCKER_IMAGE_NAME}:${IMAGE_TAG}
                        
                        # Push latest (Convenient)
                        docker push ${DOCKER_IMAGE_NAME}:latest
                    """
                }
            }
        }
    }

    // POST-BUILD ACTIONS: Runs after the pipeline finishes (Success or Failure)
    post {
        always {
            // Cleanup the local image to save disk space on the Jenkins server
            sh "docker rmi ${DOCKER_IMAGE_NAME}:${IMAGE_TAG} || true"
            sh "docker rmi ${DOCKER_IMAGE_NAME}:latest || true"
            echo "Pipeline finished. Local images cleaned up."
        }
        failure {
            echo "The build failed. Please check the logs."
        }
    }
}