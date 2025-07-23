pipeline {
  agent any

  environment {
    IMAGE_NAME = "tanmaysinghx/ts-auth-service-1625:latest"
  }

  stages {
    stage('Clone Repo') {
      steps {
        dir('backend/ts-auth-service-1625') {
          git url: 'https://github.com/tanmaysinghx/ts-auth-service-1625.git'
        }
      }
    }

    stage('Docker Build') {
      steps {
        dir('backend/ts-auth-service-1625') {
          sh 'docker build -t $IMAGE_NAME .'
        }
      }
    }

    stage('Docker Push') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          sh '''
            echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
            docker push $IMAGE_NAME
          '''
        }
      }
    }
  }

  post {
    success {
      echo "✅ Image pushed to Docker Hub: $IMAGE_NAME"
    }
    failure {
      echo "❌ Build or push failed."
    }
  }
}
