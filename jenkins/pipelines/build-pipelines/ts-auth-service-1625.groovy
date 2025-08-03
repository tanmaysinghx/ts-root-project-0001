pipeline {
  agent any

  environment {
    TS_1625_DATABASE_URL         = credentials('TS_1625_DATABASE_URL')
    TS_1625_ACCESS_TOKEN_SECRET  = credentials('TS_1625_ACCESS_TOKEN_SECRET')
    TS_1625_REFRESH_TOKEN_SECRET = credentials('TS_1625_REFRESH_TOKEN_SECRET')
    DOCKER_IMAGE_NAME            = 'tanmaysinghx/ts-auth-service-1625'
  }

  stages {
    stage('Clean Workspace') {
      steps {
        cleanWs()
      }
    }

    stage('Clone Service Repo') {
      steps {
        dir('ts-auth-service-1625') {
          git branch: 'main',
              url: 'https://github.com/tanmaysinghx/ts-auth-service-1625.git',
              credentialsId: 'github-token'
        }
      }
    }

    stage('Inspect Files') {
      steps {
        dir('ts-auth-service-1625') {
          sh '''
            echo "---- Listing ts-auth-service-1625 dir ----"
            ls -R .
            echo "------------------------------------------"
          '''
        }
      }
    }

    stage('Write .env file') {
      steps {
        dir('ts-auth-service-1625') {
          script {
            def envContent = """
DATABASE_URL="${TS_1625_DATABASE_URL}"
ACCESS_TOKEN_SECRET="${TS_1625_ACCESS_TOKEN_SECRET}"
REFRESH_TOKEN_SECRET="${TS_1625_REFRESH_TOKEN_SECRET}"
PORT=1625
API_VERSION=v2
"""
            writeFile file: '.env', text: envContent.trim()
          }
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
        withCredentials([usernamePassword(
          credentialsId: 'dockerhub-creds',
          usernameVariable: 'DOCKERHUB_USER',
          passwordVariable: 'DOCKERHUB_PASS'
        )]) {
          sh """
            echo "$DOCKERHUB_PASS" | docker login -u "$DOCKERHUB_USER" --password-stdin
            docker push ${DOCKER_IMAGE_NAME}:latest
          """
        }
      }
    }

    stage('Run Docker Container') {
      steps {
        dir('ts-auth-service-1625') {
          sh 'docker-compose down || true'
          sh 'docker-compose up -d'
        }
      }
    }
  }
}
