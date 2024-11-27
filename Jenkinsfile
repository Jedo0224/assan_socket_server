pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'parkjaeseok/asan-server'
        DOCKER_TAG = 'latest' // 필요시 Git 커밋 SHA 또는 브랜치 이름으로 변경 가능
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
                // ~/.ssh 디렉토리 생성
                sh 'mkdir -p /var/jenkins_home/.ssh'

                // known_hosts 파일 생성 및 호스트 키 추가
                sh 'ssh-keyscan -H 43.202.4.217 >> /var/jenkins_home/.ssh/known_hosts'
                // 파일 권한 설정
                sh 'chmod 644 /var/jenkins_home/.ssh/known_hosts'

                withCredentials([sshUserPrivateKey(credentialsId: 'ssh-server-credentials-id', keyFileVariable: 'SSH_KEY')]) {
                    sshagent(['ssh-server-credentials-id']) {
                        sh """
                        ssh -i ${SSH_KEY} ubuntu@43.202.4.217 <<EOF
                        docker pull ${DOCKER_IMAGE}:${DOCKER_TAG}

                        # 컨테이너가 없으면 새로운 컨테이너 생성
                        if [ ! \$(docker ps -q -f name=asan-socket-server) ]; then
                            echo "Creating and starting new container..."
                            docker run -d --name asan-socket-server -p 8080:8080 ${DOCKER_IMAGE}:${DOCKER_TAG}
                        else
                            echo "Stopping and removing existing container..."
                            docker stop asan-socket-server || true
                            docker rm -f asan-socket-server || true
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
