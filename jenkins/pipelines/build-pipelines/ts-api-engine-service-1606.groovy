pipeline {
    agent any

    environment {
        // --- PROJECT CONFIGURATION ---
        // Repo and Image updated for ts-api-engine-service-1606
        GIT_REPO_URL      = 'https://github.com/tanmaysinghx/ts-api-engine-service-1606.git'
        DOCKER_IMAGE_NAME = 'tanmaysinghx/ts-api-engine-service-1606'
        
        // Fetch the MongoDB URI safely from Jenkins Credentials
        // (Make sure ID 'TS_1606_DB_URI' exists in Manage Jenkins -> Credentials)
        TS_1606_DB_URI = credentials('TS_1606_DB_URI')
        
        // Unique tag using the Build Number
        IMAGE_TAG = "${BUILD_NUMBER}" 
    }

    stages {
        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Clone Service Repo') {
            steps {
                // Cloning into a folder named after the service
                dir('ts-api-engine-service-1606') {
                    git branch: 'main',
                        url: "${GIT_REPO_URL}",
                        credentialsId: 'github-token'
                }
            }
        }

        stage('Write .env file') {
            steps {
                dir('ts-api-engine-service-1606') {
                    script {
                        def envContent = """
PORT=1606
API_VERSION=v1
DB_URI="${TS_1606_DB_URI}"
"""
                        // Writing .env file securely
                        writeFile file: '.env', text: envContent.trim()
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                dir('ts-api-engine-service-1606') {
                    // Build specific version AND latest tag
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
                        docker push ${DOCKER_IMAGE_NAME}:${IMAGE_TAG}
                        docker push ${DOCKER_IMAGE_NAME}:latest
                    """
                }
            }
        }
    }

    post {
        always {
            // Cleanup images to save space
            sh "docker rmi ${DOCKER_IMAGE_NAME}:${IMAGE_TAG} || true"
            sh "docker rmi ${DOCKER_IMAGE_NAME}:latest || true"
        }
    }
}