package com.k_int

import groovy.util.logging.*
import org.gokb.cred.*;

@Log4j
public class HQLBuilder {

  /**
   *  Accept a qbetemplate of the form
   *  [
   *		baseclass:'Fully.Qualified.Class.Name.To.Search',
   *		title:'Title Of Search',
   *		qbeConfig:[
   *			// For querying over associations and joins, here we will need to set up scopes to be referenced in the qbeForm config
   *			// Until we need them tho, they are omitted. qbeForm entries with no explicit scope are at the root object.
   *			qbeForm:[
   *				[
   *					prompt:'Name or Title',
   *					qparam:'qp_name',
   *					placeholder:'Name or title of item',
   *					contextTree:['ctxtp':'qry', 'comparator' : 'ilike', 'prop':'name']
   *				],
   *				[
   *					prompt:'ID',
   *					qparam:'qp_id',
   *					placeholder:'ID of item',
   *					contextTree:['ctxtp':'qry', 'comparator' : 'eq', 'prop':'id', 'type' : 'java.lang.Long']
   *				],
   *				[
   *					prompt:'SID',
   *					qparam:'qp_sid',
   *					placeholder:'SID for item',
   *					contextTree:['ctxtp':'qry', 'comparator' : 'eq', 'prop':'ids.value']
   *				],
   *			],
   *			qbeResults:[
   *				[heading:'Type', property:'class.simpleName'],
   *				[heading:'Name/Title', property:'name', link:[controller:'resource',action:'show',id:'x.r.class.name+\':\'+x.r.id'] ]
   *			]
   *		]
   *	]
   *
   *
   */
  public static def build(grailsApplication, qbetemplate, params, result, target_class) {
    // select o from Clazz as o where 

    log.debug("build ${params}");

    // Step 1 : Walk through all the properties defined in the template and build a list of criteria
    def criteria = []
    qbetemplate.qbeConfig.qbeForm.each { query_prop_def ->
      if ( ( params[query_prop_def.qparam] != null ) && ( params[query_prop_def.qparam].length() > 0 ) ) {
        criteria.add([defn:query_prop_def, value:params[query_prop_def.qparam]]);
      }
    }

    def hql_builder_context = [:]
    hql_builder_context.declared_scopes = [:]
    hql_builder_context.query_clauses = []
    hql_builder_context.bindvars = [:]

    def baseclass = target_class.getClazz()
    criteria.each { crit ->
      processProperty(hql_builder_context,crit,baseclass)
      // List props = crit.def..split("\\.")
    }

    log.debug("At end of build, ${hql_builder_context}");
    hql_builder_context.declared_scopes.each { ds ->
      log.debug("Scope: ${ds}");
    }

    hql_builder_context.query_clauses.each { qc ->
      log.debug("QueryClause: ${qc}");
    }

    def hql = outputHql(hql_builder_context, qbetemplate)
    log.debug("HQL: ${hql}");
    log.debug("BindVars: ${hql_builder_context.bindvars}");
  }

  static def processProperty(hql_builder_context,crit,baseclass) {
    log.debug("processProperty ${hql_builder_context}, ${crit}");
    switch ( crit.defn.contextTree.ctxtp ) {
      case 'qry':
        processQryContextType(hql_builder_context,crit,baseclass)
        break;
    }
  }

  static def processQryContextType(hql_builder_context,crit, baseclass) {
    processQryContextType(hql_builder_context, crit, 'o', baseclass)
  }

  static def processQryContextType(hql_builder_context,crit, parent_scope, the_class) {
    List proppath = crit.defn.contextTree.prop.split("\\.")

    // Get all the combo properties defined on the class.
    def allProps = KBComponent.getAllComboPropertyDefinitionsFor(the_class)

    if ( proppath.size() > 1 ) {
      def head = proppath.remove(0)
      def newscope = parent_scope+'_'+head
      if ( hql_builder_context.declared_scopes.containsKey(newscope) ) {
        // Already established scope for this context
      }
      else {
        log.debug("Intermediate establish scope");
        establishScope(hql_builder_context, parent_scope, head, newscope)
      }
    }
    else {
      // If this is an ordinary property, add the operation. If it's a special, the make the extra joins
      Class target_class = allProps[proppath[0]]
      if ( target_class ) {
        // Combo property... We need to establish the target scope, and then add whatever the comparison is
        boolean incoming = KBComponent.lookupComboMappingFor (the_class, Combo.MAPPED_BY, proppath[0])
        log.debug("combo property, incoming=${incoming}");
        def combo_set_name = incoming ? 'incomingCombos' : 'outgoingCombos'
        def combo_prop_name = incoming ? 'fromComponent' : 'toComponent'

        // Firstly, establish a scope called proppath[0]_combo. This will be the combo link to the desired target
        def combo_scope_name = proppath[0]+"_combos"
        if ( ! hql_builder_context.declared_scopes.containsKey(combo_scope_name) ) {
          log.debug("Adding scope ${combo_scope_name}");
          establishScope(hql_builder_context, parent_scope, combo_set_name, combo_scope_name);
          def combo_type_bindvar = combo_scope_name+"_type"
          hql_builder_context.query_clauses.add("${combo_scope_name}.type = :${combo_type_bindvar}");
          hql_builder_context.bindvars[combo_type_bindvar] = RefdataCategory.lookupOrCreate ( "Combo.Type", the_class.getComboTypeValueFor (the_class, proppath[0]))
        }

        def component_scope_name = proppath[0]
        if ( ! hql_builder_context.declared_scopes.containsKey(component_scope_name) ) {
          log.debug("Adding scope ${component_scope_name}");
          establishScope(hql_builder_context, combo_scope_name, combo_prop_name, component_scope_name);
        }

        // Finally, because the leaf of the query path is a combo property, we must be being asked to match on an 
        // object.
        addQueryClauseFor(crit,hql_builder_context,component_scope_name)
      }
    }
  }

  static def establishScope(hql_builder_context, parent_scope, property_to_join, newscope_name) {
    log.debug("Establish scope ${newscope_name} as a child of ${parent_scope} property ${property_to_join}");
    hql_builder_context.declared_scopes[newscope_name] = "${parent_scope}.${property_to_join} as ${newscope_name}" 
  }

  static def addQueryClauseFor(crit, hql_builder_context, scoped_property) {
    switch ( crit.defn.contextTree.comparator ) {
      case 'eq':
        hql_builder_context.query_clauses.add("${scoped_property} = :${crit.defn.qparam}");
        hql_builder_context.bindvars[crit.defn.qparam] = crit.value
        break;
      default:
        log.error("Unhandled comparator. crit: ${crit}");
    }
  }

  static def outputHql(hql_builder_context, qbetemplate) {
    StringWriter sw = new StringWriter()
    sw.write("select o from ${qbetemplate.baseclass} as o\n")

    hql_builder_context.declared_scopes.each { scope_name,ds ->
      sw.write(" join ${ds}\n");
    }
    
    if ( hql_builder_context.query_clauses.size() > 0 ) {
      sw.write(" where");
      boolean conjunction=false
      hql_builder_context.query_clauses.each { qc ->
        if ( conjunction ) {
          // output and on second and subsequent clauses
          sw.write(" AND");
        }
        else {  
          conjunction=true
        }
        sw.write(" ");
        sw.write(qc);
        sw.write("\n");
      }
    }

    // Return the toString of the writer
    sw.toString();
  }

}
