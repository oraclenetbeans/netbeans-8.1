/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */

package org.netbeans.modules.maven.event;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author mkleint
 */
public class NbEventSpy extends AbstractEventSpy {

    private Logger logger;
    
    //#236768 guard against the mojos executing new mvn build in-JVM
    //needs to be static as the wrapped build will likely get a new instance
    //it's unlikely that we would legally trigger multiple builds inside a single jvm sequentially
    private static final AtomicBoolean insideSession = new AtomicBoolean(false);
    private static final AtomicBoolean ignoreInnerSessionEvents = new AtomicBoolean(false);
    
    @Override
    public void init(Context context) throws Exception {
        //as as by MavenCLI.java
        //data.put( "plexus", container );
        //data.put( "workingDirectory", cliRequest.workingDirectory );
        //data.put( "systemProperties", cliRequest.systemProperties );
        //data.put( "userProperties", cliRequest.userProperties );
        //data.put( "versionProperties", CLIReportingUtils.getBuildProperties() );
        super.init(context); 
    }

    @Override
    public void onEvent(Object event) throws Exception {
        //event can be:
        //org.apache.maven.execution.ExecutionEvent
        //org.sonatype.aether.RepositoryEvent
        //org.apache.maven.project.DefaultDependencyResolutionRequest
        //org.apache.maven.project.DependencyResolutionResult
        //org.apache.maven.execution.MavenExecutionRequest
        //org.apache.maven.execution.MavenExecutionResult
        //org.apache.maven.settings.building.SettingsBuildingRequest
        //org.apache.maven.settings.building.SettingsBuildingResult
        super.onEvent(event); 
        if (event instanceof ExecutionEvent) {
            ExecutionEvent ex = (ExecutionEvent) event;
            if (ignoreInnerSessionEvents.get()) { //#236768 guard against the mojos executing new mvn build in-JVM
                if (ExecutionEvent.Type.SessionEnded.equals(ex.getType())) {
                    ignoreInnerSessionEvents.set(false);
                }
                return;
            }
            JSONObject root = new JSONObject();
            
            //use base64 for complex structures or unknown values?
            {
                root.put("type", ex.getType().name());
                //the depth is as follows
                //Session -> Project -> [Fork -> ForkedProject] -> Mojo                
                
                if (ex.getProject() != null && 
                        (ExecutionEvent.Type.ProjectStarted.equals(ex.getType()) ||
                         ExecutionEvent.Type.ProjectFailed.equals(ex.getType()) ||
                         ExecutionEvent.Type.ProjectSkipped.equals(ex.getType()) ||
                         ExecutionEvent.Type.ProjectSucceeded.equals(ex.getType())
                        )) { // && not superpom
                    //only in Project* related event types
                    //project skipped called without ProjectStarted
                    MavenProject mp = ex.getProject();
                    JSONObject prj = new JSONObject();
                    prj.put("id", mp.getGroupId() + ":" + mp.getArtifactId()+ ":" + mp.getVersion());
                    if (mp.getFile() != null) { //file is null in superpom
                        prj.put("file", mp.getFile().getParentFile().getAbsolutePath());
                    }
                    root.put("prj", prj);
                }
                if (ExecutionEvent.Type.SessionStarted.equals(ex.getType()) || ExecutionEvent.Type.SessionEnded.equals(ex.getType())) {
                    //only in session events
                    root.put("prjcount", ex.getSession().getProjects().size());
                    if (ExecutionEvent.Type.SessionStarted.equals(ex.getType())) {
                        if (!insideSession.compareAndSet(false, true)) { //#236768 guard against the mojos executing new mvn build in-JVM
                            //
                            ignoreInnerSessionEvents.set(true);
                            return;
                        }
                        ClassRealm cr = ex.getSession().getContainer().getContainerRealm();
                        if (cr != null) {
                            JSONArray array = new JSONArray();
                            do {
                                URL[] urls = cr.getURLs();
                                for (URL url : urls) {
                                    array.add(url.toExternalForm());
                                }
                            } while ((cr = cr.getParentRealm()) != null);
                            root.put("mvncoreurls", array);
                        }
                    }
                    if (ExecutionEvent.Type.SessionEnded.equals(ex.getType())) {
                        insideSession.compareAndSet(true, false);//#236768 guard against the mojos executing new mvn build in-JVM
                    }
                }
                if (ex.getMojoExecution() != null && 
                        (ExecutionEvent.Type.MojoStarted.equals(ex.getType()) ||
                         ExecutionEvent.Type.MojoFailed.equals(ex.getType()) ||
                         ExecutionEvent.Type.MojoSkipped.equals(ex.getType()) || 
                         ExecutionEvent.Type.MojoSucceeded.equals(ex.getType())
                        )) {
                    //only in mojo events
                    //MojoSkipped .. only if requires online but build was offline, called without MojoStarted
                    MojoExecution me = ex.getMojoExecution();
                    JSONObject mojo = new JSONObject();
                    mojo.put("id",  me.getGroupId() + ":" + me.getArtifactId()+ ":" + me.getVersion());
                    if (me.getGoal() != null) {
                        mojo.put("goal", me.getGoal());
                    }
                    if (me.getSource() != null) {
                        mojo.put("source", me.getSource().name());
                    }
                    if (me.getExecutionId() != null) {
                        mojo.put("execId", me.getExecutionId());
                    }
                    if (me.getLifecyclePhase() != null) {
                        mojo.put("phase", me.getLifecyclePhase());
                    }
                    PluginExecution exec = me.getPlugin().getExecutionsAsMap().get(me.getExecutionId());
                    if (exec != null) {
                        InputLocation execLoc = exec.getLocation(""); //apparently getLocation("id" never returns a thing)
                        if (execLoc != null) {
                            JSONObject loc = new JSONObject();
                            loc.put("ln", execLoc.getLineNumber());
                            loc.put("col", execLoc.getColumnNumber());
                            String locS = execLoc.getSource().getLocation();
                            if (locS != null) {
                                //is path
                                loc.put("loc", locS);
                            }
                            String mid = execLoc.getSource().getModelId();
                            if (mid != null) {
                                loc.put("id", mid);
                            }
                            mojo.put("loc", loc);
                        }
                    }
                    //used to go to sources + debug build actions
                    MojoDescriptor md = me.getMojoDescriptor();
                    if (md != null) {
                        mojo.put("impl", md.getImplementation());
                        PluginDescriptor pd = md.getPluginDescriptor();
                        if (pd != null) {
                            ClassRealm cr = pd.getClassRealm();
                            if (cr != null) {
                                URL[] urls = cr.getURLs();
                                JSONArray array = new JSONArray();
                                for (URL url : urls) {
                                    array.add(url.toExternalForm());
                                }
                                mojo.put("urls", array);
                            }
                        }
                    }
                    root.put("mojo", mojo);    
                }
                if (ExecutionEvent.Type.MojoFailed.equals(ex.getType()) && ex.getException() != null) {
                    Exception exc = ex.getException();
                    if (exc instanceof LifecycleExecutionException) {
                        JSONObject excep = new JSONObject();
                        //all mojo failed events in current codebase are lifecycle execs.
                        String message = exc.getCause().getMessage();
                        byte[] enc = Base64.encodeBase64(message.getBytes("UTF-8")); //NOW are these conversions correct?
                        String encString = new String(enc, "UTF-8");
                        excep.put("msg", encString);
                        root.put("exc", excep);
                    }
                    
                }
            }    
            logger.info("NETBEANS-ExecEvent:"  + root.toString());
        }
//        if (event instanceof RepositoryEvent) {
//            RepositoryEvent re = (RepositoryEvent) event;
//            logger.info("NETBEANS-RE:" + re.getType() + ":" + re.getFile());
//        }
    }

    @Override
    public void close() throws Exception {
        super.close(); 
    }

}
