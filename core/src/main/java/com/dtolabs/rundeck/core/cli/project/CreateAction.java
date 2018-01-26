/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.dtolabs.rundeck.core.cli.project;

import com.dtolabs.rundeck.core.cli.CLIToolLogger;
import com.dtolabs.rundeck.core.dispatcher.CentralDispatcher;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import org.apache.commons.cli.CommandLine;
import org.apache.log4j.Category;

import java.io.File;
import java.io.IOException;
import java.util.Properties;


/**
 * Creates and initializes the project structure. This involves creating the project repository
 */
public class CreateAction extends BaseAction {
    static Category logger = Category.getInstance(CreateAction.class.getName());

    private boolean cygwin;
    private Properties properties;

    /**
     * Create a new CreateAction, and parse the args from the CommandLine, using {@link BaseAction#parseBaseActionArgs(org.apache.commons.cli.CommandLine)} and
     * {@link #parseCreateActionArgs(org.apache.commons.cli.CommandLine)} to create the argument specifiers.
     * @param main logger
     * @param framework framework
     * @param cli cli
     * @param properties properties
     */
    public CreateAction(final CLIToolLogger main, final IPropertyLookup framework, final CommandLine cli,
                        final Properties properties, final CentralDispatcher dispatcher) {
        this(main, framework, parseBaseActionArgs(cli), parseCreateActionArgs(cli), properties, dispatcher);
    }

    /**
     * Create a new CreateAction
     * @param main logger
     * @param framework framework object
     * @param baseArgs base args
     * @param createArgs create args
     * @param projectProperties properties
     */
    public CreateAction(final CLIToolLogger main,
                        final IPropertyLookup framework,
                        final BaseActionArgs baseArgs,
                        final CreateActionArgs createArgs,
                        final Properties projectProperties,
                        final CentralDispatcher dispatcher) {
        super(main, framework, baseArgs,dispatcher);
        properties = projectProperties;
        setCentralDispatcher(dispatcher);
        initArgs(createArgs);
    }

    /**
     * @return is cygwin
     */
    public boolean isCygwin() {
        return cygwin;
    }

    public void setCygwin(boolean cygwin) {
        this.cygwin = cygwin;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Arguments for the CreateAction
     */
    public static interface CreateActionArgs {
        /**
         * @return true if the node is using cygwin
         */
         public boolean isCygwin();
    }

    public static CreateActionArgs parseCreateActionArgs(final CommandLine cli) {
        final boolean cygwin = cli.hasOption('G');
        return new CreateActionArgs() {

            public boolean isCygwin() {
                return cygwin;
            }
        };
    }

    /**
     * Create args instance
     * @param cygwin cygwin
     * @return args
     */
    public static CreateActionArgs createArgs(final boolean cygwin){
        return new CreateActionArgs(){
            public boolean isCygwin() {
                return cygwin;
            }
        };
    }

    private void initArgs(CreateActionArgs args) {
        setCygwin(args.isCygwin());
    }

    /**
     * Execute the action.
     *
     * @throws Throwable any throwable
     */
    public void exec() throws Throwable {
        super.exec();
        if (project == null) {
            throw new IllegalStateException("project was null");

        }
        String projectsDir = frameworkProperties.getProperty(
                "framework.projects.dir"
        );
        getCentralDispatcher().createProject(
                project, generateDefaultedProperties(
                        true,
                        new File(projectsDir, project)
                )
        );
        main.log("Project was created: " + project);
    }

    private Properties generateDefaultedProperties(boolean addDefaultProps, File projectBaseDir) {
        Properties newProps = new Properties();
        if(addDefaultProps){
            if (null == properties || !properties.containsKey("resources.source.1.type") ) {
                //add default file source
                newProps.setProperty("resources.source.1.type", "file");
                newProps.setProperty(
                        "resources.source.1.config.file", new File(
                        projectBaseDir,
                        "etc/resources.xml"
                ).getAbsolutePath()
                );
                newProps.setProperty("resources.source.1.config.includeServerNode", "true");
                newProps.setProperty("resources.source.1.config.generateFileAutomatically", "true");
            }
            if(null==properties || !properties.containsKey("service.NodeExecutor.default.provider")) {
                newProps.setProperty("service.NodeExecutor.default.provider", "jsch-ssh");
            }
            if(null==properties || !properties.containsKey("service.FileCopier.default.provider")) {
                newProps.setProperty("service.FileCopier.default.provider", "jsch-scp");
            }
            if (null == properties || !properties.containsKey("project.ssh-keypath")) {
                newProps.setProperty("project.ssh-keypath", new File(System.getProperty("user.home"),
                                                                     ".ssh/id_rsa").getAbsolutePath());
            }
            if(null==properties || !properties.containsKey("project.ssh-authentication")) {
                newProps.setProperty("project.ssh-authentication", "privateKey");
            }
        }
        newProps.putAll(properties);
        return newProps;
    }


}
