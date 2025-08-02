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
    PORT = credentials('PORT_1625')
    API_VERSION = credentials('API_VERSION_1625')
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
        sh """
docker build \
  --build-arg DATABASE_URL="${env.DATABASE_URL}" \
  --build-arg ACCESS_TOKEN_SECRET="${env.ACCESS_TOKEN_SECRET}" \
  --build-arg REFRESH_TOKEN_SECRET="${env.REFRESH_TOKEN_SECRET}" \
  -t tanmaysinghx/ts-auth-service-1625:latest .
"""
      }
    }

    stage('Push Docker Image') {
      steps {
        sh 'docker push ${DOCKER_IMAGE}'
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
