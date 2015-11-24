package org.gokb.cred

import javax.persistence.Transient
import org.gokb.GOKbTextUtils
import org.gokb.DomainClassExtender
import groovy.util.logging.*

@Log4j
class TitleInstance extends KBComponent {


  // title is now NAME in the base component class...
  RefdataValue  medium
  RefdataValue  pureOA
  RefdataValue  continuingSeries
  RefdataValue  reasonRetired
  RefdataValue  OAStatus
  Date publishedFrom
  Date publishedTo
//  String imprint

  private static refdataDefaults = [
    "medium"    : "Journal",
    "pureOA"    : "No",
    "OAStatus"  : "Unknown"
  ]

  public void addVariantTitle (String title, String locale = "EN-us") {

    // Check that the variant is not equal to the name of this title first.
    if (!title.equalsIgnoreCase(this.name)) {

      // Need to compare the existing variant names here. Rather than use the equals method,
      // we are going to compare certain attributes here.
      RefdataValue title_type = RefdataCategory.lookupOrCreate("KBComponentVariantName.VariantType", "Alternate Title")
      RefdataValue locale_rd = RefdataCategory.lookupOrCreate("KBComponentVariantName.Locale", (locale))

      // Each of the variants...
      def existing = variantNames.find {
        KBComponentVariantName name = it
        return (name.locale == locale_rd && name.variantType == title_type
        && name.getVariantName().equalsIgnoreCase(title))
      }

      if (!existing) {
        addToVariantNames(
            new KBComponentVariantName([
              "variantType" : (title_type),
              "locale"    : (locale_rd),
              "status"    : RefdataCategory.lookupOrCreate('KBComponentVariantName.Status', KBComponent.STATUS_CURRENT),
              "variantName" : (title)
            ])
            )
      } else {
        log.debug ("Not adding variant title as it is the same as an existing variant.")
      }

    } else {
      log.debug ("Not adding variant title as it is the same as the actual title.")
    }
  }

  static hasByCombo = [
    issuer    : Org,
    translatedFrom  : TitleInstance,
    absorbedBy    : TitleInstance,
    mergedWith    : TitleInstance,
    renamedTo   : TitleInstance,
    splitFrom   : TitleInstance,
    imprint   : Imprint
  ]

  static manyByCombo = [
    tipps : TitleInstancePackagePlatform,
    publisher : Org,
    //        ids     :  Identifier
  ]

  static constraints = {

    medium (nullable:true, blank:false)
    pureOA (nullable:true, blank:false)
    reasonRetired (nullable:true, blank:false)
    OAStatus (nullable:true, blank:false)
//    imprint (nullable:true, blank:false)
    publishedFrom (nullable:true, blank:false)
    publishedTo (nullable:true, blank:false)
  }

  def availableActions() {
    [ [code:'method::deleteSoft', label:'Delete'],
      [code:'title::transfer', label:'Title Transfer'],
      [code:'title::change', label:'Title Change'],
      // [code:'title::reconcile', label:'Title Reconcile']
    ]
  }

  public String getNiceName() {
    return "Title";
  }

  public Org getCurrentPublisher() {
    def result = null;
    def publisher_combos = getCombosByPropertyName('publisher')
    def highest_end_date = null;

    publisher_combos.each { Combo pc ->
      if ( ( pc.endDate == null ) ||
           ( highest_end_date == null) ||
           ( pc.endDate > highest_end_date ) ) {

        if (isComboReverse('publisher')) {
          if ( pc.fromComponent.status?.value == 'Deleted' ) {
          }
          else {
            highest_end_date = pc.endDate
            result = pc.fromComponent
          }
        } else {
          if ( pc.toComponent.status?.value == 'Deleted' ) {
          }
          else {
            highest_end_date = pc.endDate
            result = pc.toComponent
          }
        }
      }
    }
    result
  }

  /**
   * Close off any existing publisher relationships and add a new one for this publiser
   */
  def changePublisher(new_publisher, boolean null_start = false) {

    if ( new_publisher != null ) {

      def current_publisher = getCurrentPublisher()

      if ( ( current_publisher != null ) && ( current_publisher.id==new_publisher.id ) ) {
        // no change... leave it be
        return false
      }
      else {
        def publisher_combos = getCombosByPropertyName('publisher')
        publisher_combos.each { pc ->
          if ( pc.endDate == null ) {
            pc.endDate = new Date();
          }
        }

        // Now create a new Combo
        RefdataValue type = RefdataCategory.lookupOrCreate(Combo.RD_TYPE, getComboTypeValue('publisher'))
        Combo combo = new Combo(
            type    : (type),
            status  : DomainClassExtender.getComboStatusActive(),
            startDate : (null_start ? null : new Date())
            )

        // Depending on where the combo is defined we need to add a combo.
        if (isComboReverse('publisher')) {
          combo.fromComponent = new_publisher
          addToIncomingCombos(combo)
        } else {
          combo.toComponent = new_publisher
          addToOutgoingCombos(combo)
        }

        new_publisher.save()
        save()

        return true
        //        publisher.add(new_publisher)
      }
    }

    // Returning false if we get here implies the publisher has not been changed.
    return false
  }


  /**
   *  refdataFind generic pattern needed by inplace edit taglib to provide reference data to typedowns and other UI components.
   *  objects implementing this method can be easily located and listed / selected
   */
  static def refdataFind(params) {
    def result = [];
    def ql = null;
    // ql = TitleInstance.findAllByNameIlike("${params.q}%",params)
    // Return all titles where the title matches (Left anchor) OR there is an identifier for the title matching what is input
    ql = TitleInstance.executeQuery("select t.id, t.name from TitleInstance as t where lower(t.name) like ? or exists ( select c from Combo as c where c.fromComponent = t and c.toComponent in ( select id from Identifier as id where id.value like ? ) )", ["${params.q?.toLowerCase()}%","${params.q}%"],[max:20]);

    if ( ql ) {
      ql.each { t ->
        result.add([id:"org.gokb.cred.TitleInstance:${t[0]}",text:"${t[1]} "])
      }
    }

    result
  }

  @Transient
  static def oaiConfig = [
    id:'titles',
    textDescription:'Title repository for GOKb',
    query:" from TitleInstance as o where o.status.value != 'Deleted'",
    pageSize:20
  ]

  /**
   *  Render this package as OAI_dc
   */
  @Transient
  def toOaiDcXml(builder, attr) {
    builder.'dc'(attr) {
      'dc:title' (name)
    }
  }

  /**
   *  Render this package as GoKBXML
   */
  @Transient
  def toGoKBXml(builder, attr) {
    def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    try {
      def tids = getIds() ?: []
      def tipps = getTipps()
      def theIssuer = getIssuer()
      def thePublisher = getPublisher()

      def history = getTitleHistory()

      builder.'gokb' (attr) {
        builder.'title' (['id':(id)]) {

          builder.'name' (name)
          builder.'imprint' (imprint?.name)
          builder.'medium' (medium?.value)
          builder.'OAStatus' (OAStatus?.value)
          builder.'continuingSeries' (continuingSeries?.value)
          builder.'publishedFrom' (publishedFrom)
          builder.'publishedTo' (publishedTo)
          builder.'issuer' (issuer?.name)

          builder.'identifiers' {
            tids?.each { tid ->
              builder.'identifier' ('namespace':tid?.namespace?.value, 'value':tid?.value)
            }
            if ( grailsApplication.config.serverUrl != null ) {
              builder.'identifier' ('namespace':'originEditUrl', 'value':"${grailsApplication.config.serverUrl}/resource/show/org.gokb.cred.TitleInstance:${id}")
            }
          }

          if ( variantNames ) {
            builder.'variantNames' {
              variantNames.each { vn ->
                builder.'variantName' ( vn.variantName )
              }
            }
          }

          if (thePublisher) {
            builder."publisher" (['id': thePublisher?.id]) {
              "name" (thePublisher?.name)
            }
          }

          if (theIssuer) {
            builder."issuer" (['id': theIssuer.id]) {
              "name" (theIssuer.name)
            }
          }

          builder.history() {
            history.each { he ->
              builder.historyEvent(['id':he.id]) {
                "date"(he.date)
                he.from.each { hti ->
                  if(hti){
                    "from" {
                      title(hti.name)
                      internalId(hti.id)
                      "identifiers" {
                        hti.getIds()?.each { tid ->
                          builder.'identifier' ('namespace':tid.namespace?.value, 'value':tid.value, 'datatype':tid.namespace.datatype?.value)
                        }

                      }
                    }
                  }
                }
                he.to.each { hti ->
                  if(hti){
                    "to" {
                      title(hti.name)
                      internalId(hti.id)
                      "identifiers" {
                        hti.getIds()?.each { tid ->
                          builder.'identifier' ('namespace':tid.namespace?.value, 'value':tid.value)
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          builder.'TIPPs' (count:tipps?.size()) {
            tipps?.each { tipp ->
              builder.'TIPP' (['id':tipp.id]) {

                def pkg = tipp.pkg
                builder.'package' (['id':pkg?.id]) {
                  builder.'name' (pkg?.name)
                }

                def platform = tipp.hostPlatform
                builder.'platform'(['id':platform?.id]) {
                  builder.'name' (platform?.name)
                }

                builder.'coverage'(
                  startDate:(tipp.startDate ? sdf.format(tipp.startDate):null),
                  startVolume:tipp.startVolume,
                  startIssue:tipp.startIssue,
                  endDate:(tipp.endDate ? sdf.format(tipp.endDate):null),
                  endVolume:tipp.endVolume,
                  endIssue:tipp.endIssue,
                  coverageDepth:tipp.coverageDepth?.value,
                  coverageNote:tipp.coverageNote)
                if ( tipp.url != null ) { 'url'(tipp.url) }
              }
            }
          }
        }
      }
    }
    catch ( Exception e ) {
      log.error("problem creating record",e);
    }
  }

  @Transient
  def getTitleHistory() {
    def result = []
    def all_related_history_events = ComponentHistoryEvent.executeQuery('select eh from ComponentHistoryEvent as eh where exists ( select ehp from ComponentHistoryEventParticipant as ehp where ehp.participant = ? and ehp.event = eh ) order by eh.eventDate',this)
    all_related_history_events.each { he ->
      def from_titles = he.participants.findAll { it.participantRole == 'in' };
      def to_titles = he.participants.findAll { it.participantRole == 'out' };

      def hint = "unknown"
      if ( ( from_titles?.size() == 1 ) && ( to_titles?.size() == 1 ) && ( from_titles[0].participant?.id != to_titles[0].participant?.id ) ) {
        hint="Rename"
      }

      result.add( [ "id":(he.id), date:he.eventDate, from:from_titles.collect{it.participant}, to:to_titles.collect{it.participant}, hint:hint ] );
    }
    return result;
  }

  def addTitlesToHistory(title, final_list, depth) {
    def result = false;
    
    if ( title ) {
      // Check to see whether this component has an id first. If not then return an empty set.
      if (title.id && title.id > 0) {
        if ( final_list.contains(title) ) {
          return;
        }
        else {
          // Find all history events relating to this title, and for each title related, add it to the final_list if it's not already in the list
          final_list.add(title)
          def all_related_history_events = ComponentHistoryEvent.executeQuery('select eh from ComponentHistoryEvent as eh where exists ( select ehp from ComponentHistoryEventParticipant as ehp where ehp.participant = ? and ehp.event = eh ) order by eh.eventDate',title)
          all_related_history_events.each { the ->
            the.participants.each { p ->
              if ( p.participant ) {
                addTitlesToHistory(p.participant, final_list, depth+1)
              }
              else {
                log.error("Title history participant was null - HistoryEvent==${the}");
              }
            }
          }
        }
      }
    }
    else {
      log.error("Attempt to addTitlesToHistory for a null title");
    }

    result;
  }

  @Transient
  def getFullTitleHistory() {
    def result = [:]
    
    // Check to see whether this component has an id first. If not then return an empty set.
    if (id && id > 0) {
      def il = []
      addTitlesToHistory(this,il,0)
      result.fh = ComponentHistoryEvent.executeQuery('select eh from ComponentHistoryEvent as eh where exists ( select ehp from ComponentHistoryEventParticipant as ehp where ehp.participant in (:titleList) and ehp.event = eh ) order by eh.eventDate asc',[titleList:il])
    }
    result;
  }

  def getPrecedingTitleId() {
    log.debug('getPrecedingTitleId')
    def preceeding_titles = []
    // Work through title history, see if there is a preceeding title...
    def ths = ComponentHistoryEvent.executeQuery('select eh from ComponentHistoryEvent as eh where exists ( select ehp from ComponentHistoryEventParticipant as ehp where ehp.participant = ? and ehp.participantRole=? and ehp.event = eh ) order by eh.eventDate desc',[this, 'out'])
    if ( ths.size() > 0 ) {
      ths[0].participants.each { p ->
        if ( p.participantRole == 'in') {
          preceeding_titles.add(p.participant.id)
        }
      }
    }
    return preceeding_titles.join(', ')
  }

}
