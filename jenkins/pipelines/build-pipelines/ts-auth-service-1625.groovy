pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'tanmaysinghx/ts-auth-service-1625:latest'
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/tanmaysinghx/ts-auth-service-1625.git', branch: 'main'
            }
        }

        stage('Install & Build in Node Container') {
            steps {
                script {
                    docker.image('node:18').inside('-u root') {
                        sh 'npm install'
                        sh 'npm run build'
                    }
                }
            }
        }

        stage('Login to Docker Hub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                    sh '''
                        echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t $DOCKER_IMAGE .'
            }
        }

        stage('Push Docker Image') {
            steps {
                sh 'docker push $DOCKER_IMAGE'
            }
        }
    }

    post {
        success {
            echo "✅ Successfully pushed image: $DOCKER_IMAGE"
        }
        failure {
            echo "❌ Build or push failed!"
        }
    }
}
