package org.gokb

import grails.plugins.springsecurity.Secured
import grails.util.GrailsNameUtils

import org.gokb.cred.*
import org.hibernate.SessionFactory;
import org.hibernate.transform.AliasToEntityMapResultTransformer

import org.gokb.cred.*


class GroupController {

  def grailsApplication
  def springSecurityService

  @Secured(['ROLE_USER', 'IS_AUTHENTICATED_FULLY'])
  def index() { 
    def result = [:]
    if ( params.id ) {
      result.group = CuratoryGroup.get(params.id);

      // get review tasks for this CG [max 20]
      // def cg_review_tasks_hql = " from ReviewRequest as rr join rr.incomingCombos where 1==2"

      // get packages for this CG [max 20]
      def cg_packages_hql = " from Package as p where exists ( select c from p.outgoingCombos as c where c.toComponent = ? and c.type.value = 'Package.CuratoryGroups')"

      result.package_count = Package.executeQuery('select count(p) '+cg_packages_hql,[result.group]);
      result.packages = Package.executeQuery('select p '+cg_packages_hql,[result.group],[max: 20, offset: 0]);
    }
    return result
  }
}