pipeline {
  agent any

  environment {
    SERVICE_DIR = 'ts-auth-service-1625'
    ENV_FILE = "${SERVICE_DIR}/.env"
  }

  stages {
    stage('Checkout Infra') {
      steps {
        git branch: 'main',
            url: 'https://github.com/tanmaysinghx/ts-root-project-0001.git',
            credentialsId: 'github-token'
      }
    }

    stage('Checkout Service') {
      steps {
        dir("${SERVICE_DIR}") {
          git branch: 'main',
              url: 'https://github.com/tanmaysinghx/ts-auth-service-1625.git',
              credentialsId: 'github-token'
        }
      }
    }

    stage('Setup .env') {
      steps {
        withCredentials([file(credentialsId: 'ts-auth.env', variable: 'ENV_SECRET')]) {
          sh 'cp $ENV_SECRET $ENV_FILE'
        }
      }
    }

    stage('Build Docker Image') {
      steps {
        dir("${SERVICE_DIR}") {
          sh 'docker compose build' // or use 'docker-compose build' if V1 is installed
        }
      }
    }

    stage('Run Docker') {
      steps {
        dir("${SERVICE_DIR}") {
          sh 'docker compose up -d'
        }
      }
    }
  }
}
