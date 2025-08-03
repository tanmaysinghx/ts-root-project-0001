pipeline {
  agent any

  environment {
    SERVICE_NAME = 'ts-auth-service-1625'
    ENV_FILE = "${SERVICE_NAME}/.env"
  }

  stages {
    stage('Checkout Root Project') {
      steps {
        git branch: 'main',
            url: 'https://github.com/tanmaysinghx/ts-root-project-0001.git',
            credentialsId: 'github-token'
      }
    }

    stage('Checkout Auth Service') {
      steps {
        dir("${SERVICE_NAME}") {
          git branch: 'main',
              url: 'https://github.com/tanmaysinghx/ts-auth-service-1625.git',
              credentialsId: 'github-token'
        }
      }
    }

    stage('Setup .env') {
  steps {
    withCredentials([file(credentialsId: 'ts-auth-env', variable: 'ENV_SECRET')]) {
      sh '''
        mkdir -p ts-auth-service-1625
        chown -R $(whoami) ts-auth-service-1625 || true
        cp "$ENV_SECRET" ts-auth-service-1625/.env
        chmod 600 ts-auth-service-1625/.env
      '''
    }
  }
}

    stage('Build Docker Image') {
      steps {
        dir("${SERVICE_NAME}") {
          sh 'docker compose build'
        }
      }
    }

    stage('Run Docker') {
      steps {
        dir("${SERVICE_NAME}") {
          sh 'docker compose up -d'
        }
      }
    }
  }
}
