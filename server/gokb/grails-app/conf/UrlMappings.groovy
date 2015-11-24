class UrlMappings {

  static mappings = {
    "/$controller/$action?/$id?"{
      constraints {
        // apply constraints here
      }
    }

    "/oai/$id"(controller:'oai',action:'index')
    
    "/packages/$packageShortcode"(controller:'packages', action:'packageContent')
    "/packages/deposit"(controller:'packages', action:'deposit')
    
    "/"(controller:'home',action:'index')
    "/rules"(controller:'home',action:'showRules')
    "/nourl"(view:'/NoUrl')
    "500"(view:'/error')
  }
}
