pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'parkjaeseok/asan-server'
        DOCKER_TAG = 'latest' // 필요시 Git 커밋 SHA 또는 브랜치 이름으로 변경 가능
    }

    stages {
        stage('Checkout Code') {
            steps {
                // Git 리포지토리에서 소스 코드 가져오기
                git branch: 'main', url: 'https://github.com/Jedo0224/assan_socket_server'
            }
        }

        stage('Build Jar') {
            steps {
                // Gradle 빌드 (Maven이라면 명령어를 변경)
                sh './gradlew clean build'
            }
        }

        stage('Build Docker Image') {
            steps {
                // Docker 이미지를 빌드
                sh """
                docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                """
            }
        }

        stage('Push Docker Image') {
            steps {
                withDockerRegistry([credentialsId: 'dockerhub-credentials-id', url: 'https://index.docker.io/v1/']) {
                    // Docker 이미지를 Docker Hub에 푸시
                    sh """
                    docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                    """
                }
            }
        }

        stage('Deploy Application') {
            steps {
                // ~/.ssh 디렉토리 생성
                sh 'mkdir -p ~/.ssh'

                // 호스트 키를 자동으로 추가
                sh """
                ssh-keyscan -H 43.202.4.217 >> ~/.ssh/known_hosts
                """
                // 파일 권한 설정
                sh 'chmod 644 ~/.ssh/known_hosts'

                withCredentials([sshUserPrivateKey(credentialsId: 'ssh-server-credentials-id', keyFileVariable: 'SSH_KEY')]) {
                    sshagent(['ssh-server-credentials-id']) {
                        sh """
                        ssh -i ${SSH_KEY} ubuntu@43.202.4.217 <<EOF
                        docker pull ${DOCKER_IMAGE}:${DOCKER_TAG}
                        docker stop asan-socket-server || true
                        docker rm asan-socket-server || true
                        docker run -d --name asan-socket-server -p 8080:8080 ${DOCKER_IMAGE}:${DOCKER_TAG}
                        EOF
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            // 빌드 성공 여부와 관계없이 클린업 수행
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
