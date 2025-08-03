pipeline {
  agent any

  environment {
    TS_1625_DATABASE_URL          = credentials('TS_1625_DATABASE_URL')
    TS_1625_ACCESS_TOKEN_SECRET   = credentials('TS_1625_ACCESS_TOKEN_SECRET')
    TS_1625_REFRESH_TOKEN_SECRET  = credentials('TS_1625_REFRESH_TOKEN_SECRET')
    DOCKER_REGISTRY_CREDENTIALS   = credentials('docker-hub-credentials') // Replace with your Docker Hub Jenkins credential ID
    DOCKER_IMAGE_NAME             = 'yourdockerhubusername/ts-auth-service-1625' // Replace with your actual image name
  }

  stages {
    stage('Checkout') {
      steps {
        git branch: 'main',
            url: 'https://github.com/tanmaysinghx/ts-auth-service-1625.git',
            credentialsId: 'github-token'
      }
    }

    stage('Write .env file') {
      steps {
        dir('ts-auth-service-1625') {
          script {
            def envContent = """
DATABASE_URL="${TS_1625_DATABASE_URL}"
ACCESS_TOKEN_SECRET=${TS_1625_ACCESS_TOKEN_SECRET}
REFRESH_TOKEN_SECRET=${TS_1625_REFRESH_TOKEN_SECRET}
PORT=1625
API_VERSION=v2
"""
            writeFile file: '.env', text: envContent.trim()
          }

          // Optional: show env preview (hide secrets)
          sh 'cat .env | grep -v SECRET'
        }
      }
    }

    stage('Docker Compose Down (Clean Reset)') {
      steps {
        dir('ts-auth-service-1625') {
          sh 'docker-compose down || true'
        }
      }
    }

    stage('Build Docker Image') {
      steps {
        dir('ts-auth-service-1625') {
          sh "docker build -t ${DOCKER_IMAGE_NAME}:latest ."
        }
      }
    }

    stage('Push to Docker Hub') {
      steps {
        script {
          docker.withRegistry('', 'docker-hub-credentials') {
            sh "docker push ${DOCKER_IMAGE_NAME}:latest"
          }
        }
      }
    }

    stage('Run Docker Container') {
      steps {
        dir('ts-auth-service-1625') {
          sh 'docker-compose up -d'
        }
      }
    }
  }
}
