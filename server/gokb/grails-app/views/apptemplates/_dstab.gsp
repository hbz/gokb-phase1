<table id="tab-decision-suppport" class="table table-bordered">
    <thead>
    <tr>
        <th>Criterion</th>
        <th>Value</th>
        <th>Notes</th>
    </tr>
    </thead>
    <tbody>
    <g:each in="${d.decisionSupportLines.values()}" var="dsl">
        <tr>
            <td colspan="3" style="vertical-align:top;">
                <h3>${dsl.description}</h3></td>
        </tr>
        <g:each in="${dsl.criterion}" var="id, c">
            <tr>
                <td style="vertical-align:top; white-space: nowrap; width: 25%;">
                    <div class="criterionTitle">&nbsp;&nbsp;${c['title']}</div></br>

                    <div class="vote" style="white-space: nowrap;">
                        <g:if test="${!c['masterVote'].isEmpty()}">
                            <div id="masterVote" title="" class="DSVote DSMasterVote">
                                <b>Master Vote</b></br>
                                <span class="DSAuthor">JISC</span>
                                <span id="${c['appliedTo']}_${id}_master" ${c['masterVote'][0]=='Green'?'class="text-positive"': (c['masterVote'][0]=='Red' ? 'class="text-negative"' : 'class="text-contentious"')} ><i class="fa fa-times-circle fa-2x"></i></span> &nbsp;
                                        </br></br></br>
                                    </div>
                        </g:if>

                        <g:if test="${c['yourVote'].isEmpty()}">
                            <i id="${c['appliedTo']}_${id}_q_neutral" class="text-neutral">
                                <span class="fa fa-question-circle fa-2x"></span><span>&nbsp;&nbsp;cast your vote</span></i></br>
                                    </br>
                        </g:if>
                        <g:else>
                            <span><b>You have voted</b></span>
                        </g:else>
                        <div class="DSVote" id="currentVote${c['appliedTo']}_${id}">
                            <a id="${c['appliedTo']}_${id}_r_negative" title="${c['voteCounter'][0] +  (c['voteCounter'][3]>0? ' Red vote(s) and ' + c['voteCounter'][3] + ' commented only':' Red vote(s)') }" href='#' ${c['yourVote'][0]=='Red'?'class="text-negative selected"':''} ><i class="fa fa-times-circle fa-2x"></i></a> &nbsp;
                            <a id="${c['appliedTo']}_${id}_a_contentious" title="${c['voteCounter'][1] +  (c['voteCounter'][3]>0? ' Amber vote(s) and ' + c['voteCounter'][3] + ' commented only':' Amber vote(s)') }" href='#' ${c['yourVote'][0]=='Amber'?'class="text-contentious selected"':''} ><i class="fa fa-info-circle fa-2x"></i></a>&nbsp;
                            <a id="${c['appliedTo']}_${id}_g_positive" title="${c['voteCounter'][2] +  (c['voteCounter'][3]>0? ' Green vote(s) and ' + c['voteCounter'][3] + ' commented only':' Green vote(s)') }" href='#' ${c['yourVote'][0]=='Green'?'class="text-positive selected"':''} ><i class="fa fa-check-circle fa-2x"></i></a>
                        </div>
                    </br></br>
                        <g:if test="${c['otherVotes'].isEmpty()}">
                            No one else has voted yet
                        </g:if>
                        <g:elseif test="${grailsApplication.config.feature.otherVoters}">
                            <table id="otherVoters" style="margin: 0; padding: 0">
                                <thead>
                                <tr>
                                    <th>Others Voted</th>
                                </tr>
                                </thead>
                                <tbody>
                                <g:each in="${c['otherVotes']}" var="o">
                                    <tr>
                                        <td>
                                            <p class="DSAuthor DSInlineBlock" title="${o[2]?.org?.name}">
                                                ${o[2]?.displayName}
                                            </p>
                                        </td>
                                        <td>
                                            <p class="DSVote DSInlineBlock">
                                                <span id="${c['appliedTo']}_${id}_r_negative" ${o[0]=='Red'?'class="text-negative"':''} ><i class="fa fa-times-circle fa-2x"></i></span> &nbsp;
                                                <span id="${c['appliedTo']}_${id}_a_contentious"  ${o[0]=='Amber'?'class="text-contentious"':''} ><i class="fa fa-info-circle fa-2x"></i></span>&nbsp;
                                                <span id="${c['appliedTo']}_${id}_g_positive" ${o[0]=='Green'?'class="text-positive"':''} ><i class="fa fa-check-circle fa-2x"></i></span>
                                                <g:if test="${o[0]=='Unknown'}"><i>(Commented only)</i></g:if>
                                            </p>
                                        </td>
                                    </tr>
                                </g:each>
                                </tbody>
                            </table>
                        </g:elseif>
                        <g:else>
                            <div id="otherVoters">
                                <table>
                                    <thead>
                                    <tr>
                                        <th colspan="3">Others Voted</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <tr>
                                        <td><a id="otherRed"  href='#' class="text-negative"><i class="fa fa-times-circle fa-2x"></i></a> &nbsp;</td>
                                        <td><a id="OtherYellow"  href='#' class="text-contentious" ><i class="fa fa-info-circle fa-2x"></i></a>&nbsp;</td>
                                        <td><a id="OtherGreen"  href='#' class="text-positive"><i class="fa fa-check-circle fa-2x"></i></a></td>
                                    </tr>
                                    <tr>
                                        <td><span class="badge">${c['voteCounter'][0]}</span></td>
                                        <td><span class="badge">${c['voteCounter'][1]}</span></td>
                                        <td><span class="badge">${c['voteCounter'][2]}</span></td>
                                    </tr>
                                    </tbody>
                                </table>
                            </div>
                        </g:else>
                    </div>
                </td>

                <td style="vertical-align:top; width:75%;">
                    %{--Setup each note--}%
                    <dl id="${c['appliedTo']}_${id}_notestable">
                        <g:each in="${c['notes']}" var="note">
                            <dt>
                                <span class="DSAuthor">${note?.criterion?.user.username}</span>-
                            <g:if test="${note?.criterion?.value?.value == 'Unknown'}">-(NOT VOTED)--</g:if>
                                <i class="DSTimestamp">
                                    <g:if test="${note.lastUpdated == note.dateCreated}"><g:formatDate date="${note.dateCreated}" /></g:if>
                                    <g:else>Edited: <g:formatDate date="${note.lastUpdated}" /></g:else>
                                </i>
                            </dt>
                            <dd>
                                <p class="DSInlineBlock DSOrg">
                                    <g:if test="${note.criterion?.user?.org?.name == null}">
                                        N/A
                                    </g:if>
                                    <g:else>
                                        ${note.criterion?.user?.org?.name}
                                    </g:else>
                                </p>
                                <g:if test="${note?.isDeleted}">
                                    <p data-cid="${c['appliedTo']}_${id}" class="triangle-border DSInlineBlock text-deleted border-deleted">
                                </g:if>
                                <g:elseif test="${note?.criterion?.value?.value == null || note?.criterion?.value?.value == 'Amber'}" >
                                    <p class="triangle-border DSInlineBlock text-contentious border-contentious">
                                </g:elseif>
                                <g:elseif test="${note?.criterion?.value?.value == 'Red'}">
                                    <p class="triangle-border DSInlineBlock text-negative border-negative">
                                </g:elseif>
                                <g:elseif test="${note?.criterion?.value?.value == 'Green'}">
                                    <p class="triangle-border DSInlineBlock text-positive border-positive">
                                </g:elseif>
                                <g:elseif test="${note?.criterion?.value?.value == 'Unknown'}">
                                    <p class="triangle-border DSInlineBlock text-contentious border-contentious">
                                </g:elseif>

                                <g:if test="${!note.isDeleted && note.criterion.user.id == user.id}" >
                                    <g:xEditable owner="${note}" field="note"/>
                                    <a data-comp="${c['appliedTo']}_${id}" data-note="${note.id}" class="noteDelete text-negative fa fa-times-circle fa-2x"></a>
                                </g:if>
                                <g:else>
                                    ${note.note}
                                </g:else>
                            </p> %{--closing tag from dynamic check of colour--}%
                            </dd>
                        </g:each>
                    </dl>

                    <form role="form" class="form" onsubmit='return addNote("${c['appliedTo']}_${id}", "${user.username}", "${user?.org?.name}")'>
                        <div class="form-group">
                            <div class="input-group">
                                <span class="input-group-addon">
                                    Add note
                                </span>
                                <textarea class="form-control" id="${c['appliedTo']}_${id}_newnote"></textarea>
                                <span class="input-group-addon">
                                    <button type="submit">Add</button>
                                </span>
                            </div>
                        </div>
                    </form>
                </td>
            </tr>
        </g:each>
    </g:each>

    </tbody>
</table>
