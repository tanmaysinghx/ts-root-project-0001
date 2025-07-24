pipeline {
    agent any

    environment {
        IMAGE_NAME = "tanmaysinghx/ts-auth-service-1625:latest"
        CONTAINER_NAME = "ts-auth-service-1625-dev"
        EXPOSED_PORT = "1625"
        INTERNAL_PORT = "1625"
    }

    stages {
        stage('Pull Image') {
            steps {
                sh "docker pull $IMAGE_NAME"
            }
        }

        stage('Copy .env') {
            steps {
                sh 'cp /var/jenkins_home/envs/ts-auth-service-1625.env .env'
            }
        }

        stage('Unpack Aiven CA Cert') {
            steps {
                withCredentials([file(credentialsId: 'aiven-ca-cert', variable: 'AIVEN_CA')]) {
                    sh '''
                        mkdir -p certs
                        cp "$AIVEN_CA" certs/ca.pem
                    '''
                }
            }
        }

        stage('Run Container with Cert') {
            steps {
                sh '''
                    docker rm -f $CONTAINER_NAME || true

                    docker run -d --name $CONTAINER_NAME \
                      --env-file .env \
                      -v $PWD/certs:/certs \
                      -p $EXPOSED_PORT:$INTERNAL_PORT \
                      $IMAGE_NAME
                '''
            }
        }
    }

    post {
        always {
            sh 'rm -rf .env certs || true'
        }
    }
}
