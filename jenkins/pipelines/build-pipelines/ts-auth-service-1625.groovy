pipeline {
  agent any

  environment {
    TS_1625_DATABASE_URL         = credentials('TS_1625_DATABASE_URL')
    TS_1625_ACCESS_TOKEN_SECRET  = credentials('TS_1625_ACCESS_TOKEN_SECRET')
    TS_1625_REFRESH_TOKEN_SECRET = credentials('TS_1625_REFRESH_TOKEN_SECRET')
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

        // Optional: verify content (without secrets)
        sh 'cat .env | grep -v SECRET'
      }
    }

    stage('Build Docker Image') {
      steps {
        sh 'docker-compose build'
      }
    }

    stage('Run Docker Container') {
      steps {
        sh 'docker-compose up -d'
      }
    }
  }
}
