pipeline {
    agent any

    environment {
        // --- PROJECT CONFIGURATION ---
        // UPDATE THESE: Repo URL for your Gamer Portfolio
        GIT_REPO_URL      = 'https://github.com/tanmaysinghx/ts-umt-ui-1780.git'
        
        // Docker Hub Image Name
        DOCKER_IMAGE_NAME = 'tanmaysinghx/ts-umt-ui-1780'
        
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
                // Cloning into a specific folder to keep structure clean
                dir('ts-umt-ui-1780') {
                    // Update 'github-token' ID if using a different credential ID in Jenkins
                    git branch: 'main',
                        url: "${GIT_REPO_URL}",
                        credentialsId: 'github-token'
                }
            }
        }

        // NOTE: The 'Write .env file' stage from your sample was removed.
        // This is a static Angular frontend served by Nginx, so it doesn't 
        // typically consume runtime .env files like a Node.js backend.

        stage('Build Docker Image') {
            steps {
                dir('ts-umt-ui-1780') {
                    // Build specific version AND latest tag
                    // ensure your Dockerfile exposes port 6969 as configured previously
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
            // Cleanup images to save space on the Jenkins agent
            sh "docker rmi ${DOCKER_IMAGE_NAME}:${IMAGE_TAG} || true"
            sh "docker rmi ${DOCKER_IMAGE_NAME}:latest || true"
        }
    }
}