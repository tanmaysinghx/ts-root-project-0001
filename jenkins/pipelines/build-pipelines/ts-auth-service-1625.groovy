pipeline {
  agent any

  environment {
    ENV_FILE = 'ts-auth-service-1625/.env'
  }

  stages {
    stage('Checkout') {
      steps {
        git branch: 'main',
            url: 'https://github.com/tanmaysinghx/ts-auth-service-1625.git',
            credentialsId: 'github-token'
      }
    }

    stage('Setup .env') {
      steps {
        withCredentials([file(credentialsId: 'ts-auth-env', variable: 'ENV_SECRET')]) {
          sh 'cp $ENV_SECRET $ENV_FILE'
        }
      }
    }

    stage('Build Docker Image') {
      steps {
        dir('ts-auth-service-1625') {
          sh 'docker-compose build'
        }
      }
    }

    stage('Run or Push') {
      steps {
        dir('ts-auth-service-1625') {
          sh 'docker-compose up -d'
        }
      }
    }
  }
}
        
