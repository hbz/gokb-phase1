package org.gokb

import java.util.Map;
import java.util.Set;

import au.com.bytecode.opencsv.CSVReader
import au.com.bytecode.opencsv.bean.CsvToBean
import au.com.bytecode.opencsv.bean.HeaderColumnNameMappingStrategy
import au.com.bytecode.opencsv.bean.HeaderColumnNameTranslateMappingStrategy

import com.k_int.ConcurrencyManagerService;
import com.k_int.ConcurrencyManagerService.Job
import com.k_int.ClassUtils

import grails.transaction.Transactional

import org.gokb.cred.BookInstance
import org.gokb.cred.ComponentPerson
import org.gokb.cred.ComponentSubject
import org.gokb.cred.IngestionProfile
import org.gokb.cred.Identifier
import org.gokb.cred.IdentifierNamespace
import org.gokb.cred.KBComponent
import org.gokb.cred.KBComponentVariantName
import org.gokb.cred.KBartRecord
import org.gokb.cred.Org
import org.gokb.cred.Package;
import org.gokb.cred.Person
import org.gokb.cred.Platform
import org.gokb.cred.RefdataCategory;
import org.gokb.cred.ReviewRequest
import org.gokb.cred.Subject
import org.gokb.cred.TitleInstance;
import org.gokb.cred.TitleInstancePackagePlatform;
import org.gokb.cred.User;

@Transactional
class TSVIngestionService {

  def grailsApplication
  def titleLookupService
  def componentLookupService
  def refdataCategory
  def sessionFactory
  def propertyInstanceMap = org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin.PROPERTY_INSTANCE_MAP


  /* This class is a BIG rip off of the TitleLookupService, it really should be
   * refactored to use inheritance of something!
   */

  private Map class_one_match (def ids) {
    // Get the class 1 identifier namespaces.
    Set<String> class_one_ids = grailsApplication.config.identifiers.class_ones
    def xcheck = grailsApplication.config.identifiers.cross_checks
    // Return the list of class 1 identifiers we have found or created, as well as the
    // list of matches
    def result = [
    "class_one"         : false,
    "ids"                : [],
    "matches"           : [] as Set,
    "x_check_matches"   : [] as Set
    ]

    // Go through each of the class_one_ids and look for a match.
    ids.each { id_def ->
    if (id_def.type && id_def.value) {
      log.debug("id_def.type")
      // id_def is map with keys 'type' and 'value'
      Identifier the_id = Identifier.lookupOrCreateCanonicalIdentifier(id_def.type, id_def.value)
      // Add the id.
      result['ids'] << the_id
      log.debug("${result['ids']}")
      // We only treat a component as a match if the matching Identifer
      // is a class 1 identifier.
      if (class_one_ids.contains(id_def.type)) {
      log.debug("class one contains")
      // Flag class one is present.
      result['class_one'] = true
      // Flag for title match
      boolean title_match = false
      // If we find an ID then lookup the components.
      Set<KBComponent> comp = the_id.identifiedComponents
      comp.each { KBComponent c ->
        // Ensure we're not looking at a Hibernate Proxy class representation of the class
        KBComponent dproxied = ClassUtils.deproxy(c);
        // Only add if it's a title.
        if ( dproxied instanceof BookInstance ) {
        title_match = true
        result['matches'] << (dproxied as BookInstance)
        }
      }
      // Did the ID yield a Title match?
      log.debug("title match ${title_match}")
      if (!title_match) {
        log.debug ("No class one ti match.")
        // We should see if the current ID namespace should be cross checked with another.
        def other_ns = null
        for (int i=0; i<xcheck.size() && (!(other_ns)); i++) {
        Set<String> test = xcheck[i]
        if (test.contains(id_def.type)) {
          // Create the set then remove the matched instance to test teh remaining ones.
          other_ns = new HashSet<String>(test)
          // Remove the current namespace.
          other_ns.remove(id_def.type)
          log.debug ("Cross checking for ${id_def.type} in ${other_ns.join(", ")}")
          Identifier xc_id = null
          for (int j=0; j<other_ns.size() && !(xc_id); j++) {
          String ns = other_ns[j]
          IdentifierNamespace namespace = IdentifierNamespace.findByValue(ns)
          if (namespace) {
            // Lookup the identifier namespace.
            xc_id = Identifier.findByNamespaceAndValue(namespace, id_def.value)
            log.debug ("Looking up ${ns}:${id_def.value} returned ${xc_id}.")
            comp = xc_id?.identifiedComponents
            comp?.each { KBComponent c ->
            // Ensure we're not looking at a Hibernate Proxy class representation of the class
            KBComponent dproxied = ClassUtils.deproxy(c);
            // Only add if it's a title.
            if ( dproxied instanceof BookInstance ) {
              log.debug ("Found ${id_def.value} in ${ns} namespace.")
              // Save details here so we can raise a review request, only if a single title was matched.
              result['x_check_matches'] << [
              "suppliedNS"  : id_def.type,
              "foundNS"     : ns
              ]
              result['matches'] << (dproxied as BookInstance)
            }
            }
          }
          }
        }
        }
      }
      }
    }
    }
    result
  }

  def find (String title, def identifiers, def user = null, def project = null) {
    // The TitleInstance
    BookInstance the_title = null
    log.debug("${title}")
    if (title == null) return null
    // Create the normalised title.
    String norm_title = GOKbTextUtils.generateComparableKey(title)
    // Lookup any class 1 identifier matches
    def results = class_one_match (identifiers)
    // The matches.
    List< KBComponent> matches = results['matches'] as List
    switch (matches.size()) {
    case 0 :
      // No match behaviour.
      log.debug ("Title class one identifier lookup yielded no matches.")
      // Check for presence of class one ID
      if (results['class_one']) {
      log.debug ("One or more class 1 IDs supplied so must be a new TI.")
      // Create the new TI.
      the_title = new BookInstance(name:title)
      } else {
      // No class 1s supplied we should try and find a match on the title string.
      log.debug ("No class 1 ids supplied.")
      // Lookup using title string match only.
      the_title = attemptStringMatch (norm_title)
      if (the_title) {
        log.debug("TI ${the_title} matched by name. Partial match")
        // Add the variant.
        the_title.addVariantTitle(title)
        // Raise a review request
        ReviewRequest.raise(
          the_title,
          "'${title}' added as a variant of '${the_title.name}'.",
          "No 1st class ID supplied but reasonable match was made on the title name.",
          user, project
          )
      } else {
        log.debug("No TI could be matched by name. New TI, flag for review.")
        // Could not match on title either.
        // Create a new TI but attach a Review request to it.
        the_title = new BookInstance(name:title)
        ReviewRequest.raise(
          the_title,
          "New TI created.",
          "No 1st class ID supplied and no match could be made on title name.",
          user, project
          )
      }
      }
      break;
    case 1 :
      // Single component match.
      log.debug ("Title class one identifier lookup yielded a single match.")
      // We should raise a review request here if the match was made by cross checking
      // different identifier namespaces.
      if (results['x_check_matches'].size() == 1) {
      def data = results['x_check_matches'][0]
      // Fire the review request.
      ReviewRequest.raise(
        matches[0],
        "Identifier type mismatch.",
        "Ingest file ${data['suppliedNS']} matched an existing ${data['foundNS']}.",
        user,
        project
      )
      }
      // Now we can examine the text of the title.
      the_title = singleTIMatch(title, norm_title, matches[0], user, project)
      break;
    default :
      // Multiple matches.
      log.debug ("Title class one identifier lookup yielded ${matches.size()} matches. This is a bad match. Ingest should skip this row.")
      break;
    }

    // If we have a title then lets set the publisher and ids...
    if (the_title) {
    // The current IDs of the title
    def current_ids = the_title.getIds()
    results['ids'].each {
      if ( ! current_ids.contains(it) ) {
      current_ids.add(it);
      }
    }
    log.debug(the_title.getIds())
    // Try and save the result now.
    if ( the_title.save(failOnError:true, flush:true) ) {
      log.debug("Succesfully saved TI: ${the_title.name} (This may not change the db)")
    }
    else {
      the_title.errors.each { e ->
      log.error("Problem saving title: ${e}");
      }
    }
    }
    the_title
  }

  //for now, we can only do authors. (kbart limitation)
  def BookInstance addPerson (person_name, role, ti, user=null, project = null) {
    if (person_name) {
      def person = componentLookupService.findComponents('org.gokb.cred.Person', person_name)
    log.debug("this was found for person: ${person}");
    switch(person.size()) {
      case 0:
        log.debug("Person lookup yielded no matches.")
        def the_person = new Person(name:person_name)
          if (the_person.save(failOnError:true, flush:true)) {
          log.debug("saved ${the_person.name}")
          person << the_person
          ReviewRequest.raise(
          ti,
          "'${the_person}' added as ${role.value} of '${ti.name}'.",
           "This person did not exist before, so has been newly created",
          user, project)
          } else {
            the_person.errors.each { error ->
          log.error("problem saving ${the_person.name}:${error}")
          }
          }
      case 1:
        def people = ti.getPeople()?:[]
        log.debug("ti.getPeople ${people}")
        // Has the person ever existed in the list against this title.
        boolean done=false;
        for (cp in people) {
          if (!done && (cp.person.id==person[0].id &&
                  cp.role.id==role.id) ) {
            done=true;
            }
        }
        if (!done) {
        def componentPerson = new ComponentPerson(component:ti,
                                person:person,
                              role:role)
          log.debug("people did not contain this person")
          // First person added?
          boolean not_first = people.size() > 0
        boolean added = componentPerson.save(failOnError:true, flush:true);
        if (!added) {
          componentPerson.errors.each { error ->
          log.error("problem saving ${componentPerson}:${error}")
          }
        }
          // Raise a review request, if needed.
          if (not_first && added) {
          ReviewRequest.raise(
            ti,
            "Added '${person.name}' as ${role.value} on '${ti.name}'.",
            "Person supplied in ingested file is additional to any already present on BI.",
            user, project)
        }
      }
      break
      default:
      log.debug ("Person lookup yielded ${person.size()} matches. Not really sure which person to use, so not using any.")
      break
    }
    }
    ti
  }

  def BookInstance addSubjects(the_subjects, the_title) {
    if (the_subjects) {
      for (the_subject in the_subjects) {
        def subject = Subject.findAllByNameIlike(the_subject) //no alt names for subjects
        log.debug("this was found for subject: ${subject}")
        if (!subject) {
          log.debug("subject not found, creating a new one")
          subject = new Subject(name:the_subject)
          subject.save()
        }
        boolean done=false
        def componentSubjects = the_title.subjects?:[]
        for (cs in componentSubjects) {
          if (!done && (cs.subject.id==subject.id)) {
            done=true;
          }
        }
        if (!done) {
          def cs = new ComponentSubject(component:the_title,
                                      subject:subject);
          cs.save()
        }
      }
    }
    the_title
  }

  def BookInstance addPublisher (publisher_name, ti, user = null, project = null) {
    if ( publisher_name != null ) {
    def publisher = componentLookupService.findComponents('org.gokb.cred.Org',publisher_name)
    log.debug("this was found for publisher: ${publisher}");
    // Found a publisher.
    switch (publisher.size()) {
      case 0:
      log.debug ("Publisher lookup yielded no matches.")
      def the_publisher = new Org(name:publisher_name)
      if (the_publisher.save(failOnError:true, flush:true)) {
        log.debug("saved ${the_publisher.name}")
        publisher << the_publisher
        ReviewRequest.raise(
          ti,
          "'${the_publisher}' added as a publisher of '${ti.name}'.",
           "This publisher did not exist before, so has been newly created",
          user, project)
      } else {
        the_publisher.errors.each { error ->
          log.error("problem saving ${the_publisher.name}:${error}")
      }
      }

      //carry on...
      case 1:
        log.debug("found a publisher")
        def orgs = ti.getPublisher()
        log.debug("ti.getPublisher ${orgs}")
        // Has the publisher ever existed in the list against this title.
        if (!orgs.contains(publisher[0])) {
        log.debug("orgs did not contain this puboisher")
        // First publisher added?
        boolean not_first = orgs.size() > 0
        // Added a publisher?
        log.debug("calling changepublisher")
        boolean added = ti.changePublisher (
        publisher[0],
        true
        )
        log.debug(not_first)
        log.debug(added)
        // Raise a review request, if needed.
        if (not_first && added) {
          ReviewRequest.raise(
          ti,
          "Added '${publisher.name}' as a publisher on '${ti.name}'.",
          "Publisher supplied in ingested file is different to any already present on TI.",
          user, project)
        }
        } //!orgs.contains(publisher)
        break
      default:
        log.debug ("Publisher lookup yielded ${publisher.size()} matches. Not really sure which publisher to use, so not using any.")
      break
    }  //switch
    } //publisher_name !=null
    ti
  }

  private BookInstance attemptStringMatch (String norm_title) {

    // Default to return null.
    BookInstance ti = null

    // Try and find a title by matching the norm string.
    // Default to the min threshold
    double best_distance = grailsApplication.config.cosine.good_threshold

    BookInstance.list().each { BookInstance t ->

    // Get the distance and then determine whether to add to the list or
    double distance = GOKbTextUtils.cosineSimilarity(norm_title, GOKbTextUtils.generateComparableKey(t.getName()))
    if (distance >= best_distance) {
      ti = t
      best_distance = distance
    }

    t.variantNames?.each { vn ->
      distance = GOKbTextUtils.cosineSimilarity(norm_title, vn.normVariantName)
      if (distance >= best_distance) {
      ti = t
      best_distance = distance
      }
    }
    }

    // Return what we have found... If anything.
    ti
  }

  private TitleInstance singleTIMatch(String title, String norm_title, TitleInstance ti, User user, project = null) {
    // The threshold for a good match.
    double threshold = grailsApplication.config.cosine.good_threshold
    // Work out the distance between the 2 title strings.
    double distance = GOKbTextUtils.cosineSimilarity(GOKbTextUtils.generateComparableKey(ti.getName()), norm_title)
    // Check the distance.
    switch (distance) {
    case 1 :
      // Do nothing just continue using the TI.
      log.debug("Exact distance match for TI.")
      break
    case {
      ti.variantNames.find {alt ->
      GOKbTextUtils.cosineSimilarity(GOKbTextUtils.generateComparableKey(alt.variantName), norm_title) >= threshold
      }}:
      // Good match on existing variant titles
      log.debug("Good match for TI on variant.")
      break
    case {it >= threshold} :
      // Good match. Need to add as alternate name.
      log.debug("Good distance match for TI. Add as variant.")
      ti.addVariantTitle(title)
      break
    default :
      // Bad match...
      ti.addVariantTitle(title)
      // Raise a review request
      ReviewRequest.raise(
        ti,
        "'${title}' added as a variant of '${ti.name}'.",
        "Match was made on 1st class identifier but title name seems to be very different.",
        user, project
        )
      break
    }

    ti
  }

  //these are now ingestions of profiles.
  def ingest(the_profile, datafile, job=null) {

     def start_time = System.currentTimeMillis();

    log.debug("TSV ingestion called")

    try {

      def ingest_date = new Date();
      log.debug("Ingest date is ${ingest_date}")

      job?.setProgress(0)
      //not suer we need this totaslly...

      def kbart_beans=[]

      //we kind of assume that we need to convert to kbart
      if ("${the_profile.packageType}"!='kbart2') {
        kbart_beans = convertToKbart(the_profile, datafile)
      } else {
        kbart_beans = getKbartBeansFromKBartFile(datafile)
      }


      log.debug("Ingesting ${kbart_beans.size} rows")
      //now its converted, ingest it into the database.
      for (int x=0; x<kbart_beans.size;x++) {
        log.debug("Ingesting ${x} of ${kbart_beans.size}")
        TitleInstance.withNewTransaction {
          writeToDB(kbart_beans[x], the_profile, datafile, ingest_date )
          if ( x % 50 == 0 ) {
            cleanUpGorm();
          }
        }
        job?.setProgress( (x/kbart_beans.size()*100) as int)
      }

      // the_profile.save(flush:true)
      def the_package=handlePackage(the_profile)

      log.debug("Expunging old tipps [Tipps belonging to ${the_package} last seen prior to ${ingest_date}] - ${the_profile.packageName}");
      try {

        TitleInstancePackagePlatform.withNewTransaction {
          // Find all tipps in this package which have a lastSeen before the ingest date
          def q = TitleInstancePackagePlatform.executeQuery('select tipp '+
                           'from TitleInstancePackagePlatform as tipp, Combo as c '+
                           'where c.fromComponent=? and c.toComponent=tipp and tipp.lastSeen < ? and tipp.accessEndDate is null',
                          [the_package,ingest_date]);

          q.each { tipp ->
            log.debug("Soft delete missing tipp ${tipp.id} - last seen was ${tipp.lastSeen}, ingest date was ${ingest_date}");
            // tipp.deleteSoft()
            tipp.accessEndDate = new Date();
            tipp.save()
          }
          log.debug("Completed tipp cleanup")
        }
      }
      catch ( Exception e ) {
        log.error("Problem",e)
      }
      finally {
        log.debug("Done")
      }
    }
    catch ( Exception e ) {
      log.error("Problem",e)
    }

    job?.setProgress(100)

    def elapsed = System.currentTimeMillis()-start_time;

    log.debug("ingest completed in ${elapsed}ms");
  }

  //this method does a lot of checking, and then tries to save the title to the DB.
  def writeToDB(the_kbart, the_profile, the_datafile, ingest_date) {
    //simplest method is to assume that everything is new.
    //however the golden rule is to check that something already exists and then
    //re-use it.
    log.debug("TSVINgestionService:writeToDB")
    //first we need a platform:
    URL platform_url=new URL(the_kbart.title_url?:the_profile.platformUrl)
    def platform = handlePlatform(platform_url.host, the_profile.source)
    if (platform!=null) {
      def the_package=handlePackage(the_profile)
      if (the_package!=null) {
        log.debug(the_kbart.online_identifier)
        def identifiers = []
        identifiers << [type:'isbn', value:the_kbart.online_identifier]
        the_kbart.additional_isbns.each { identifier ->
          identifiers << [type: 'isbn', value:identifier]
        }
        if ( identifiers.size() > 0 ) {
          log.debug("looking for book using these identifiers: ${identifiers}")
          def title = find(the_kbart.publication_title, identifiers)
          title.source=the_profile.source
          log.debug("title found: for ${the_kbart.publication_title}:${title}")
          def author_role = RefdataCategory.lookupOrCreate(grailsApplication.config.kbart2.personCategory,
                                      grailsApplication.config.kbart2.authorRole)
          def editor_role = RefdataCategory.lookupOrCreate(grailsApplication.config.kbart2.personCategory,
                                     grailsApplication.config.kbart2.editorRole)

          if (title) {
            addOtherFieldsToTitle(title, the_kbart)
            addPublisher(the_kbart.publisher_name, title)
            addPerson(the_kbart.first_author, author_role, title);
            addPerson(the_kbart.first_editor, editor_role, title);
            addSubjects(the_kbart.subjects, title)
            the_kbart.additional_authors.each { author ->
              addPerson(author, author_role, title)
            }
            processTIPPS(the_profile.source, the_datafile, the_kbart, the_package, title, platform, ingest_date)
          } else {
             log.warn("problem getting the title...")
          }

        }
        else {
          log.debug("Skipping row - no identifiers")
        }

      } else {
      log.warn("couldn't reslove package - title not added.");
      }
    } else {
      log.warn("couldn't reslove platform - title not added.");
    }
  }

  def addOtherFieldsToTitle(title, the_kbart) {
    title.medium=RefdataCategory.lookupOrCreate("TitleInstance.Medium", "eBook")
    log.debug("title.medium set")
    title.editionNumber=the_kbart.monograph_edition
    log.debug("title.editionNumber set")
    title.dateFirstInPrint=parseDate(the_kbart.date_monograph_published_print)
    log.debug("first date in print set")
    title.dateFirstOnline=parseDate(the_kbart.date_monograph_published_online)
    log.debug("first online date set")
    title.volumeNumber=the_kbart.monograph_volume
    log.debug("save title")
    title.save()

  }

  Date parseDate(String datestr) {
    // Parse the date.
    Date the_date = null
    if (datestr) {
      log.debug ("Trying to parse date from ${datestr}")
            try {
      the_date = ISODateParser.parseDateTime(datestr).toDate()
      } catch (Throwable t) {
        log.debug ("Error parsing date. '${datestr}' resulted in null date.")
        // Ensure null date.
      the_date = null
      }
    }
    the_date
  }

  def processTIPPS(the_source,
                   the_datafile,
                   the_kbart,
                   the_package,
                   the_title,
                   the_platform,
                   ingest_date) {

    log.debug("TSVIngestionService::processTIPPS with ${the_package}, ${the_title}, ${the_platform}")

    //first, try to find the platform. all we have to go in the host of the url.
    def tipp_values = [
      url:the_kbart.title_url?:'',
      // pkg:the_package,
      // title:the_title,
      // hostPlatform:the_platform,
      embargo:the_kbart.embargo_info?:'',
      coverageNote:the_kbart.coverage_depth?:'',
      notes:the_kbart.notes?:'',
      // source:the_source,
      accessStartDate:ingest_date,
      lastSeen:ingest_date
    ]

    def tipp=null

    // ToDo : This should be a query, not iterating through in memory - or it will be incrementally slower
    //tipp = the_title.getTipps().find { def the_tipp ->
      // Filter tipps for matching pkg and platform.
    //  boolean matched = the_tipp.pkg == the_package
    //  matched = matched && the_tipp.hostPlatform == the_platform
    //  matched
    // }
    def tipps = TitleInstance.executeQuery('select tipp from TitleInstancePackagePlatform as tipp, Combo as pkg_combo, Combo as title_combo, Combo as platform_combo  '+
                                           'where pkg_combo.toComponent=tipp and pkg_combo.fromComponent=?'+
                                           'and platform_combo.toComponent=tipp and platform_combo.fromComponent = ?'+
                                           'and title_combo.toComponent=tipp and title_combo.fromComponent = ?',
                                          [the_package,the_platform,the_title])
    if ( tipps.size() == 1 ) {
      tipp = tipps[0]
    }


    if (tipp==null) {
      log.debug("create a new tipp as at ${ingest_date}");

      // These are immutable for a TIPP - only set at creation time
      tipp_values.pkg = the_package;
      tipp_values.title = the_title;
      tipp_values.hostPlatform = the_platform;
      tipp_values.source = the_source;
      tipp = TitleInstancePackagePlatform.tiplAwareCreate(tipp_values)
    } else {
      log.debug("found a tipp to use")

      // Set all properties on the object.
      tipp_values.each { prop, value ->
        // Only set the property if we have a value.
        if (value != null && value != "") {
          tipp."${prop}" = value
        }
      }
    }

    if ( ingest_date ) {
      log.debug("Update last seen on tipp ${tipp.id} - set to ${ingest_date}")
      tipp.lastSeen = ingest_date;
    }

    log.debug("save tipp")
    tipp.save(failOnError:true, flush:true)
    if (!the_datafile.tipps.find {_tipp->_tipp.id==tipp.id}) {
      the_datafile.tipps << tipp
    }
    the_datafile.save(flush:true)
    log.debug("processTIPPS returning")
  }

  //this is a lot more complex than this for journals. (which uses refine)
  //theres no notion in here of retiring packages for example.
  //for this v1, I've made this very simple - probably too simple.
  def handlePackage(the_profile) {
    def result;
    def packages=Package.findAllByNameIlike(the_profile.packageName);
    switch (packages.size()) {
      case 0:
        //no match. create a new platform!
        result = new Package(name:the_profile.packageName, source:the_profile.source)
        if (result.save(flush:true, failOnError:true)) {
          log.debug("saved new package: ${result}")
        } else {
          for (error in result.errors) {
            log.error(error);
          }
        }
        break;
      case 1:
        //found a match
        result=packages[0]
        log.debug("match package found: ${result}")
      break
      default:
        log.error("found multiple packages when looking for ${the_profile.packageName}")
      break
    }
    result;
  }

  def handlePlatform(host, the_source) {
    def result;
    def platforms=Platform.findAllByPrimaryUrlIlike(host);
    switch (platforms.size()) {
      case 0:
        //no match. create a new platform!
        result = new Platform(name:host, primaryUrl:host, source:the_source)
        if (result.save(flush:true, failOnError:true)) {
          log.debug("saved new platform: ${result}")
        } else {
          for (error in result.errors) {
            log.error(error);
          }
        }
        break;
      case 1:
        //found a match
        result=platforms[0]
        log.debug("match platform found: ${result}")
      break
      default:
        log.error("found multiple platforms when looking for ${host}")
      break
    }
    result;
  }

  //note- don't do the additional fields just yet, these will need to be mapped in
  def getKbartBeansFromKBartFile(the_data) {
    log.debug("kbart2 file, so use CSV to Bean") //except that this doesn't always work :(
    def results=[]
    //def ctb = new CsvToBean<KBartRecord>()
    //def hcnms = new HeaderColumnNameMappingStrategy<KBartRecord>()
    //hcnms.type = KBartRecord
    def csv = new CSVReader(new InputStreamReader(new ByteArrayInputStream(the_data.fileData)),'\t' as char)
    //results=ctb.parse(hcnms, csv)
    //quick check that results aren't null...


    Map col_positions=[:]
    String[] header = csv.readNext()
    int ctr = 0
    header.each {
      //col_positions[it]=-1
      col_positions[it]=ctr++
    }
    log.debug("${col_positions}")
    String[] nl=csv.readNext()
    while(nl!=null) {
      Map result=[:]
      for (key in col_positions.keySet()) {
        log.debug("Checking ${key}")
        if ( key && key.length() > 0 ) {
          //so, springer files seem to start with a dodgy character (int) 65279
          if (((int)key.toCharArray()[0])==65279) {
            def index=key.getAt(1..key.length()-1)
            //if ( ( col_positions[key] ) && ( nl.length < col_positions[key] ) ) {
              result[index]=nl[col_positions[key]]
            //}
          } else {
            //if ( ( col_positions[key] ) && ( nl.length < col_positions[key] ) ) {
              result[key]=nl[col_positions[key]]
            //}
          }
        }

      }

      log.debug(result)
      log.debug(new KBartRecord(result))

      //this is a cheat cos I don't get why springer files don't work!


      results<<new KBartRecord(result)
      nl=csv.readNext()
    }
    log.debug(results)
    results
  }

  def convertToKbart(the_profile, data_file) {
    def results = []
    log.debug("in convert to Kbart2")
    log.debug("file package type is ${the_profile.packageType}")

    //need to know the file type, then we need to create a new data structure for it
    //in the config, need to map the fields from the formats we support into kbart.

    def fileRules = grailsApplication.config.kbart2.mappings."${the_profile.packageType}"

    if (fileRules==null) {
      throw new Exception("couldn't find file rules for ${the_profile.packageType}")
    }
    //can you read a tsv file?
    CSVReader csv = new CSVReader(new InputStreamReader(new ByteArrayInputStream(data_file.fileData)),'\t' as char)
        Map col_positions=[:]
    fileRules.each { fileRule ->
      col_positions[fileRule.field]=-1;
    }
        String [] header = csv.readNext()
        int ctr = 0
    header.each {
          col_positions [ it ] = ctr++
        }
    String [] nl = csv.readNext()
    while ( nl != null ) {
      KBartRecord result = new KBartRecord()
      fileRules.each { fileRule ->
        boolean done=false
        log.debug(fileRule.field)
        String data = nl[col_positions[fileRule.field]];
        log.debug(data)
        if (fileRule.separator!=null && data.indexOf(fileRule.separator)>-1) {
          def parts = data.split(fileRule.separator)
          data=parts[0]
          if ( parts.size() > 1 && fileRule.additional!=null ) {
            for (int x=1; x<parts.size(); x++) {
              result[fileRule.additional] << parts[x]
            }
            done=true
          }
        }
        if (fileRule.additional!=null && !done) {
          result[fileRule.additional] << data
        } else {
          result[fileRule.kbart]=data
        }
      }
      log.debug(result)
      results<<result;
      nl=csv.readNext()
    }
    results
  }

  def cleanUpGorm() {
    log.debug("Clean up GORM");

    // Get the current session.
    def session = sessionFactory.currentSession

    // flush and clear the session.
    session.flush()
    session.clear()

    // Clear the property instance map.
    propertyInstanceMap.get().clear()
  }


}
