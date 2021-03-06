#!/usr/bin/env groovy

pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                dir('server')
                {
                    dir('gokb'){
                    sh 'grails refresh-dependencies --non-interactive'
                    sh 'grails war --non-interactive ${JENKINS_HOME}/war_files/${BRANCH_NAME}_${BUILD_NUMBER}.war'
                    }
                }
                echo 'Building..'
            }
        }
        stage('Test') {
            steps {
                echo 'Testing..'
            }
        }
        stage('Deploy') {
            steps {

            script{
                    env.SERVERDEPLOY = input message: 'On which Server you want to deploy', ok: 'Deploy!',
                                            parameters: [choice(name: 'Server to deploy', choices: "${GOKB_DEV}\n${GOKB_QA}\n${GOKB_PROD}", description: '')]
                    echo "Server Set to: ${SERVERDEPLOY}"
                    echo "Deploying on ${SERVERDEPLOY}...."
                }

                input('OK to continuethe Deploying on Server ${env.SERVERDEPLOY}?')
                script{
                        currentBuild.displayName = "${currentBuild.number}: Deploy on ${SERVERDEPLOY}"
                        sh 'cp ${JENKINS_HOME}/war_files/${BRANCH_NAME}_${BUILD_NUMBER}.war ${WORKSPACE}/gokb.war'
                        writeFile file: "${WORKSPACE}/job.batch", text: "put /${WORKSPACE}/gokb.war\n quit"
                        sh 'sftp -b ${WORKSPACE}/job.batch -i ${TOMCAT_HOME_PATH}/.ssh/id_rsa ${SERVERDEPLOY}:${TOMCAT_HOME_PATH}/default/webapps/'

                }

            }
        }
        stage('Post-Build'){
            steps {
                echo 'Post-Build'
            }
        }
    }
    post {
            success {
                echo 'I succeeeded!'
                script{
                        env.changeLog = "No Changes"
                      if(currentBuild.changeSets){
                        env.changeLog = "Change Log:\n\n\n"
                        echo 'Change Log'
                        def changeLogSets = currentBuild.changeSets
                                for (int i = 0; i < changeLogSets.size(); i++) {
                                        def entries = changeLogSets[i].items
                                        for (int j = 0; j < entries.length; j++) {
                                            def entry = entries[j]
                                            echo "${entry.commitId} by ${entry.author} on ${new Date(entry.timestamp)}: ${entry.msg}"
                                            env.changeLog += "${entry.commitId} by ${entry.author} on ${new Date(entry.timestamp)}: ${entry.msg}\n"
                                        }
                                    }
                                }
                    }

                mail to: 'moetez.djebeniani@hbz-nrw.de, philipp.boeselager@hbz-nrw.de',
                                                             subject: "Succeeded Deploy on Server ${SERVERDEPLOY}: ${currentBuild.fullDisplayName}",
                                                             body: "Succeeded Deploy on Server ${SERVERDEPLOY}  \nAll Right: ${env.BUILD_URL} \n\n\n${changeLog}"
                cleanWs()
            }
            unstable {
                echo 'I am unstable :/'
            }
            failure {
                echo 'I failed :('
                mail to: 'moetez.djebeniani@hbz-nrw.de',
                             subject: "Failed Deploy on Server ${SERVERDEPLOY}: ${currentBuild.fullDisplayName}",
                             body: "Failed Deploy on Server ${SERVERDEPLOY}\n Something is wrong with ${env.BUILD_URL}"
            }
            changed {
                echo 'Things were different before...'
            }
    }
}