pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                echo '代码已拉取'
            }
        }
        stage('Build') {
            steps {
                sh 'echo "执行构建..."'
            }
        }
        stage('Test') {
            steps {
                sh 'echo "执行测试..."'
            }
        }
    }

    post {
        success {
            echo '流水线执行成功'
        }
        failure {
            echo '流水线执行失败'
        }
    }
}