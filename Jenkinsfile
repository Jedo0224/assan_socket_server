pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'parkjaeseok/asan-server'
        DOCKER_TAG = 'latest'
    }

    stages {
        stage('Checkout Code') {
            steps {
                git branch: 'main', url: 'https://github.com/Jedo0224/assan_socket_server'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                """
            }
        }

        stage('Push Docker Image') {
            steps {
                withDockerRegistry([credentialsId: 'dockerhub-credentials-id', url: 'https://index.docker.io/v1/']) {
                    sh """
                    docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                    """
                }
            }
        }

        stage('Deploy Application') {
            steps {
                sh 'mkdir -p /var/jenkins_home/.ssh'
                sh 'ssh-keyscan -H 43.202.4.217 >> /var/jenkins_home/.ssh/known_hosts'
                sh 'chmod 644 /var/jenkins_home/.ssh/known_hosts'

                withCredentials([sshUserPrivateKey(credentialsId: 'ssh-server-credentials-id', keyFileVariable: 'SSH_KEY')]) {
                    sshagent(['ssh-server-credentials-id']) {
                        sh """
                        ssh -i ${SSH_KEY} ubuntu@43.202.4.217 <<EOF
                        docker pull ${DOCKER_IMAGE}:${DOCKER_TAG}

                        # 컨테이너가 없으면 새로운 컨테이너를 생성하고, 기존 컨테이너가 있으면 중지하고 삭제 후 새로 생성
                        if [ ! \$(docker ps -q -f name=asan-socket-server) ]; then
                            echo "Creating and starting new container..."
                            docker run -d --name asan-socket-server -p 8080:8080 ${DOCKER_IMAGE}:${DOCKER_TAG}
                        else
                            echo "Stopping and removing existing container..."
                            docker stop asan-socket-server || true
                            docker rm asan-socket-server || true
                            docker run -d --name asan-socket-server -p 8080:8080 ${DOCKER_IMAGE}:${DOCKER_TAG}
                        fi
                        EOF
                        """
                    }
                }
            }
        }
    }
    post {
        always {
            cleanWs()
        }
        success {
            echo 'Application deployed successfully!'
        }
        failure {
            echo 'Pipeline failed. Please check the logs.'
        }
    }
}
