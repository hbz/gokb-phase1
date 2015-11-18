package org.gokb.cred

import org.hibernate.proxy.HibernateProxy

class RefdataValue {

  String value
  String icon
  String description
  String sortKey
  RefdataValue useInstead

  static belongsTo = [
    owner:RefdataCategory
  ]

  static mapping = {
    id column:'rdv_id'
    version column:'rdv_version'
    owner column:'rdv_owner', index:'rdv_entry_idx'
    value column:'rdv_value', index:'rdv_entry_idx'
    description column:'rdv_desc'
    sortKey column:'rdv_sortkey'
    useInstead column:'rdv_use_instead'
    icon column:'rdv_icon'
  }

  static constraints = {
    icon(nullable:true, blank:true)
    description(nullable:true, blank:true, maxSize:64)
    useInstead(nullable:true, blank:false)
    sortKey(nullable:true, blank:false)
  }

  @Override
  public String toString() {
    return "${value}"
  }

  @Override
  public boolean equals (Object obj) {

    if (obj != null) {
      if ( obj instanceof RefdataValue ) {
        return obj.id == id
      }
      else if ( obj instanceof HibernateProxy ) {
        Object dep_obj = KBComponent.deproxy (obj)
        return dep_obj.id == id
      }
    }

    return false
  }

  static def refdataFind(params) {
    def result = [];
    def ql = null;
    // ql = RefdataValue.findAllByValueIlikeOrDescriptionIlike("%${params.q}%","%${params.q}%",params)
    // ql = RefdataValue.findWhere("%${params.q}%","%${params.q}%",params)

    def query = "from RefdataValue as rv where rv.useInstead is null and lower(rv.value) like ?"
    def query_params = ["%${params.q.toLowerCase()}%"]

    if ( ( params.filter1 != null ) && ( params.filter1.length() > 0 ) ) {
      query += ' and rv.owner.desc = ?'
      query_params.add(params.filter1);
    }

    query += ' order by rv.sortKey, rv.description, rv.id'

    ql = RefdataValue.findAll(query, query_params, params)

    if ( ql ) {
      ql.each { id ->
        result.add([id:"${id.class.name}:${id.id}",text:"${id.value} - ${id.description?:''}"])
      }
    }

    result
  }
  
  static def refdataCreate(String... obj_def) {
    if (obj_def.length == 4) {
      String type = obj_def[2]
      String val = obj_def[3]
      
      return RefdataCategory.lookupOrCreate(obj_def[2], obj_def[3])
    }
    
    return null
  }

  //  def availableActions() {
  //    [ [ code:'object::delete' , label: 'Delete' ] ]
  //  }
}
