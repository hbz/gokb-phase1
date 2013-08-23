<!DOCTYPE html>
<html>
  <head>
    <meta name="layout" content="main"/>
    <r:require modules="gokbstyle"/>
    <title>GOKb</title>
  </head>
  <body>

   <div class="container-fluid">
     <div class="row-fluid">
       <div id="openActivities" class="span6 well">
         <g:if test="${(openActivities != null ) && ( openActivities.size() > 0 )}">
           <h3>Currently open activities</h3>
           <table class="table table-striped">
             <thead>
               <tr>
                 <td>Activity</td>
                 <td>Type</td>
                 <td>Created</td>
                 <td>Last Updated</td>
               </tr>
             </thead>
             <tbody>
               <g:each in="${openActivities}" var="activity">
                 <tr>
                   <td><g:link controller="workflow" action="${activity.activityAction}" id="${activity.id}">${activity.activityName?:'No name'}</g:link></td>
                   <td>${activity.type}</td>
                   <td>${activity.dateCreated}</td>
                   <td>${activity.lastUpdated}</td>
                 </tr>
               </g:each>
             </tbody>
           </table>
         </g:if>
         <g:else>
           <h3>No active open activities</h3>
         </g:else>

         <g:if test="${(recentlyClosedActivities != null ) && ( recentlyClosedActivities.size() > 0 )}">
           <h3>Recently Closed activities</h3>
           <table class="table table-striped">
             <thead>
               <tr>
                 <td>Activity</td>
                 <td>Type</td>
                 <td>Created</td>
                 <td>Last Updated</td>
               </tr>
             </thead>
             <tbody>
               <g:each in="${recentlyClosedActivities}" var="activity">
                 <tr>
                   <td><g:link controller="workflow" action="${activity.activityAction}" id="${activity.id}">${activity.activityName?:'No name'}</g:link></td>
                   <td>${activity.type}</td>
                   <td>${activity.dateCreated}</td>
                   <td>${activity.lastUpdated}</td>
                 </tr>
               </g:each>
             </tbody>
           </table>
         </g:if>
         <g:else>
           <h3>No active open activities</h3>
         </g:else>

       </div>
       <div id="recentActivity" class="span6">
       </div>
     </div>
   </div>
  
  </body>
</html>
