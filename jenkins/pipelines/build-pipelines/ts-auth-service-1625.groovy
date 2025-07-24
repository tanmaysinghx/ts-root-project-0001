pipeline {
  agent any

  environment {
    DOCKER_IMAGE = 'tanmaysinghx/ts-auth-service-1625:latest'

    // Secrets stored in Jenkins Credentials as "Secret Text"
    DATABASE_URL = credentials('TS_1625_DATABASE_URL')
    ACCESS_TOKEN_SECRET = credentials('TS_1625_ACCESS_TOKEN_SECRET')
    REFRESH_TOKEN_SECRET = credentials('TS_1625_REFRESH_TOKEN_SECRET')
    DOCKER_USERNAME = credentials('DOCKER_USERNAME')
    DOCKER_PASSWORD = credentials('DOCKER_PASSWORD')
  }

  stages {
    stage('Checkout Code') {
      steps {
        git branch: 'main', url: 'https://github.com/tanmaysinghx/ts-auth-service-1625.git'
      }
    }

    stage('Generate .env') {
      steps {
        writeFile file: '.env', text: """
DATABASE_URL=${DATABASE_URL}
ACCESS_TOKEN_SECRET=${ACCESS_TOKEN_SECRET}
REFRESH_TOKEN_SECRET=${REFRESH_TOKEN_SECRET}
"""
      }
    }

    stage('Docker Login') {
      steps {
        sh "echo '${DOCKER_PASSWORD}' | docker login -u '${DOCKER_USERNAME}' --password-stdin"
      }
    }

    stage('Build Docker Image') {
      steps {
        // Build image without copying .env
        sh 'docker build --network=host -t ${DOCKER_IMAGE} .'
      }
    }

    stage('Push Docker Image') {
      steps {
        sh 'docker push ${DOCKER_IMAGE}'
      }
    }

    stage('Deploy Container') {
      steps {
        sh 'docker stop ts-auth-service-1625 || true && docker rm ts-auth-service-1625 || true'
        sh 'docker run -d --name ts-auth-service-1625 --env-file .env -p 1625:1625 ${DOCKER_IMAGE}'
      }
    }
  }

  post {
    always {
      sh 'rm -f .env || true'
    }
    success {
      echo '✅ Deployment successful.'
    }
    failure {
      echo '❌ Deployment failed.'
    }
  }
}
