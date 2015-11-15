package pages

import geb.Page
import geb.error.RequiredPageContentNotPresent
// import grails.plugin.remotecontrol.*

class BasePage extends Page {

    // def remote = new RemoteControl()

    // String getMessage(String code, Object[] args = null, Locale locale=null) {
    //         remote.exec { ctx.messageSource.getMessage(code, args, locale) }
    // }
 static content = {

    waitElement {run ->
      try{
          waitFor{run()}
      } catch (geb.waiting.WaitTimeoutException e) {
          report "Problem "+e.toString()
          throw new RequiredPageContentNotPresent()
      }
    }

  }
}
