<!DOCTYPE html>
<html>
<head>
<meta name="layout" content="sb-admin" />
<title>GOKb: ${displayobj?.getNiceName() ?: 'Component'} 
&lt;${ displayobj?.isEditable() ? 'Editable' : 'Read Only' }&gt; 
&lt;${ displayobj?.isCreatable() ? 'Creatable' : 'Not Creatable' }&gt;
</title>
</head>
<body>
	<h1 class="page-header">
		<g:render template="component_heading" contextPath="../apptemplates" model="${[d:displayobj]}" />
	</h1>
	<div id="mainarea" class="panel panel-default">
		<div class="panel-body">
			<g:if test="${displayobj != null}">
			  <g:if test="${ (!displayobj.respondsTo("isEditable")) || displayobj.isEditable() }" >
				  <g:if test="${ !((request.curator != null ? request.curator.size() > 0 : true) || (params.curationOverride == "true")) }" >
				    <div class="col-xs-3 pull-right well">
				      <div class="alert alert-warning">
                <h4>Warning</h4>
                <p>You are not a curator of this component. You can still edit it, but please contact a curator before making major changes.</p>
                <p><g:link class="btn btn-danger" controller="${ params.controller }" action="${ params.action }" id="${ displayobj.className }:${ displayobj.id }" params="${ (request.param ?: [:]) + ["curationOverride" : true] }" >Confirm and switch to edit mode</g:link></p>
              </div>
            </div>
				  </g:if>
					<g:elseif test="${displayobj.respondsTo('availableActions')}">
						<div class="col-xs-3 pull-right well" id="actionControls">
							<g:form controller="workflow" action="action" method="post"
								class='action-form'>
								<h4>Available actions</h4>
								<input type="hidden"
									name="bulk:${displayobj.class.name}:${displayobj.id}"
									value="true" />
								<div class="input-group">
									<select id="selectedAction" name="selectedBulkAction" class="form-control" >
										<option value="">-- Select an action to perform --</option>
										<g:each var="action" in="${displayobj.availableActions()}">
											<option value="${action.code}">
												${action.label}
											</option>
										</g:each>
									</select>
									<span class="input-group-btn">
										<button type="submit" class="btn btn-default" >Go</button>
									</span>
								</div>
							</g:form>
						</div>
					</g:elseif>
			  </g:if>
				<g:if test="${displaytemplate != null}">
					<g:if test="${displaytemplate.type=='staticgsp'}">
						<g:render template="${displaytemplate.rendername}"
							contextPath="../apptemplates"
							model="${[d:displayobj, rd:refdata_properties, dtype:displayobjclassname_short]}" />
					</g:if>
				</g:if>
			</g:if>
			<g:else>
				<h1>Unable to find record in database : Please notify support</h1>
			</g:else>
		</div>
	</div>

<asset:script type="text/javascript">
  var hash = window.location.hash;
  hash && $('ul.nav a[href="' + hash + '"]').tab('show');

  $('.nav-tabs a').click(function (e) {
    $(this).tab('show');
    var scrollmem = $('body').scrollTop();
    window.location.hash = this.hash;
    $('html,body').scrollTop(scrollmem);
  });
</asset:script>

</body>
</html>
