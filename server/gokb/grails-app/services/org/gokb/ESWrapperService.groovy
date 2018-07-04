package org.gokb

import org.elasticsearch.client.Client
import org.elasticsearch.node.Node
import static org.elasticsearch.node.NodeBuilder.nodeBuilder
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.groovy.*
import org.elasticsearch.common.transport.InetSocketTransportAddress

class ESWrapperService {

  static transactional = false

  def grailsApplication
  def esclient = null;

  @javax.annotation.PostConstruct
  def init() {

    log.debug("ESWrapperService::init");

    def es_cluster_name = grailsApplication.config.aggr_es_cluster  ?: "elasticsearch"
    def es_host         = grailsApplication.config.aggr_es_hostname ?: "localhost"

    log.debug("es_cluster = ${es_cluster_name}");
    log.debug("es_host = ${es_host}");

    Settings settings = Settings.settingsBuilder()
                         .put("client.transport.sniff", true)
                         .put("cluster.name", es_cluster_name)
                         .build();

    esclient = TransportClient.builder().settings(settings).build();

    // add transport addresses
    esclient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(es_host), 9300 as int))

    log.debug("ES Init completed");
  }

  def getClient() {
    return esclient
  }

  @javax.annotation.PreDestroy
  def destroy() {
    log.debug("Destroy");
  }

}
