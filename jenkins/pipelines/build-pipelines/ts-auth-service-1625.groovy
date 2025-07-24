pipeline {
    agent any

    environment {
        SERVICE_REPO = 'https://github.com/tanmaysinghx/ts-auth-service-1625.git'
        SERVICE_DIR = 'ts-auth-service-1625'
        DOCKER_IMAGE = 'tanmaysinghx/ts-auth-service'
    }

    stages {
        stage('Clone ts-auth-service') {
            steps {
                git url: "${SERVICE_REPO}", branch: 'main'
            }
        }

        stage('Install & Build in Node Docker') {
            steps {
                sh '''
                    docker run --rm -v $PWD:/app -w /app node:18 \
                    sh -c "npm install && npm run build"
                '''
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t ts-auth-service:latest .'
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                    sh '''
                        echo "$PASSWORD" | docker login -u "$USERNAME" --password-stdin
                        docker tag ts-auth-service:latest $DOCKER_IMAGE:latest
                        docker push $DOCKER_IMAGE:latest
                    '''
                }
            }
        }
    }

    post {
        failure {
            echo "❌ Build or push failed."
        }
        success {
            echo "✅ Successfully built and pushed Docker image: $DOCKER_IMAGE:latest"
        }
    }
}
