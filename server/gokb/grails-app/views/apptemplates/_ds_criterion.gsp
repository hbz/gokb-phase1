<dl class="dl-horizontal">
  <dt> <g:annotatedLabel owner="${d}" property="title">Title</g:annotatedLabel> </dt>
  <dd> <g:xEditable class="ipe" owner="${d}" field="title" /> </dd>
  <dt> <g:annotatedLabel owner="${d}" property="description">Description</g:annotatedLabel> </dt>
  <dd> <g:xEditable class="ipe" owner="${d}" field="description" /> </dd>
  <dt> <g:annotatedLabel owner="${d}" property="explanation">Explanation</g:annotatedLabel> </dt>
  <dd> <g:xEditable class="ipe" owner="${d}" field="explanation" /> </dd>

  <table class="table table-bordered">
    <thead>
      <tr>
        <th>Item</th>
        <th>Applied Criteria</th>
        <th>Notes</th>
      </tr>
    </thead>
    <tbody>
      <g:each in="${d.getDecisionSupportLines()}" var="ac">
        <tr>
          <td>${ac.appliedTo?.getNiceName()}</td>
          <td>
            ${ac.value?.value}
            <i class="fa fa-question-circle fa-2x" style="color:${(ac.value?.value=='Unknown'||ac.value?.value==null)?'blue':'grey'};"></i>&nbsp;
            <i class="fa fa-times-circle fa-2x" style="color:${ac.value?.value=='Red'?'red':'grey'};"></i> &nbsp;
            <i class="fa fa-info-circle fa-2x" style="color:${ac.value?.value=='Amber'?'#FFBF00':'grey'};"></i>&nbsp;
            <i class="fa fa-check-circle fa-2x" style="color:${ac.value?.value=='Green'?'green':'grey'};"></i>
          </td>
          <td>
            <ul>
              <g:each in="${ac.notes?.each}" var="note">
                <li>${note.note}</li>
              </g:each>
            </ul>
          </td>
        </tr>
      </g:each>
    </tbody>
  </table>

</dl>
