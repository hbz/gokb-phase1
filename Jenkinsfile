#!/usr/bin/env groovy

pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                dir('app')
                {
                    sh 'grails refresh-dependencies --non-interactive'
                    sh 'grails war --non-interactive ${JENKINS_HOME}/war_files/${BRANCH_NAME}_${BUILD_NUMBER}.war'
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
                    env.SERVERDELOPY = input message: 'On which Server you want to deploy', ok: 'Deploy!',
                                            parameters: [choice(name: 'Server to deploy', choices: "${GOKB_DEV}\n${GOKB_QA}\n${GOKB_PROD}", description: '')]
                    echo "Server Set to: ${SERVERDELOPY}"
                    echo "Deploying on ${SERVERDELOPY}...."
                }

                input('OK to continue?')
                script{

                        sh 'cp ${JENKINS_HOME}/war_files/${BRANCH_NAME}_${BUILD_NUMBER}.war ${WORKSPACE}/ROOT.war'
                        writeFile file: "${WORKSPACE}/job.batch", text: "put /${WORKSPACE}/ROOT.war\n quit"
                        sh 'sftp -b ${WORKSPACE}/job.batch -i ${TOMCAT_HOME_PATH}/.ssh/id_rsa ${SERVERDELOPY}:${TOMCAT_HOME_PATH}/default/webapps/'

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

                mail to: 'moetez.djebeniani@hbz-nrw.de',
                                                             subject: "Succeeded Deploy on Server ${SERVERDELOPY}: ${currentBuild.fullDisplayName}",
                                                             body: "Succeeded Deploy on Server ${SERVERDELOPY}  \nAll Right: ${env.BUILD_URL} \n\n\n${changeLog}"
                cleanWs()
            }
            unstable {
                echo 'I am unstable :/'
            }
            failure {
                echo 'I failed :('
                mail to: 'moetez.djebeniani@hbz-nrw.de',
                             subject: "Failed Deploy on Server ${SERVERDELOPY}: ${currentBuild.fullDisplayName}",
                             body: "Failed Deploy on Server ${SERVERDELOPY}\n Something is wrong with ${env.BUILD_URL}"
            }
            changed {
                echo 'Things were different before...'
            }
    }
}