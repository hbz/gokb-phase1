#!groovy

@groovy.transform.BaseScript(GOKbSyncBase)
import GOKbSyncBase


while ( moredata ) {
  
  def resources = []
  fetchFromSource (path: '/gokb/oai/platforms') { resp, body ->

    body?.'ListRecords'?.'record'.metadata.gokb.platform.eachWithIndex { data, index ->

      println("Record ${index + 1}")

      def resourceFieldMap = addCoreItems ( data )
      
      resourceFieldMap['platformName'] = cleanText(data.name.text())
      resourceFieldMap['platformUrl'] = cleanText(data.primaryUrl.text())
      
      
      directAddFields (data, ['authentication', 'software', 'service', 'provider'], resourceFieldMap)
      
      resources.add(resourceFieldMap)
    }
  }
  
  resources.each {
    sendToTarget (path: '/gokb/integration/crossReferencePlatform', body: it)
  }
  
  // Save the config.
  saveConfig()
}