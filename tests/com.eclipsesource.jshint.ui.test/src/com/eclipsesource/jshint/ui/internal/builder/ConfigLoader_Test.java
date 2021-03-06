/*******************************************************************************
 * Copyright (c) 2013 EclipseSource.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ralf Sternberg - initial implementation and API
 ******************************************************************************/
package com.eclipsesource.jshint.ui.internal.builder;

import org.eclipse.core.resources.IProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.jshint.ui.internal.preferences.OptionsPreferences;
import com.eclipsesource.jshint.ui.internal.preferences.PreferencesFactory;
import com.eclipsesource.jshint.ui.test.TestUtil;
import com.eclipsesource.json.JsonObject;

import static com.eclipsesource.jshint.ui.test.TestUtil.createProject;
import static com.eclipsesource.jshint.ui.test.TestUtil.deleteProject;
import static org.junit.Assert.*;


public class ConfigLoader_Test {

  private IProject project;
  private OptionsPreferences workspacePrefs;
  private OptionsPreferences projectPrefs;

  @Before
  public void setUp() {
    project = createProject( "test" );
    workspacePrefs = new OptionsPreferences( PreferencesFactory.getWorkspacePreferences() );
    projectPrefs = new OptionsPreferences( PreferencesFactory.getProjectPreferences( project ) );
  }

  @After
  public void tearDown() {
    deleteProject( project );
  }

  @Test
  public void usesWorkspaceOptionsByDefault() {
    workspacePrefs.setOptions( "a: 1, b: 1" );
    createProjectConfig( new JsonObject().add( "b", 2 ).add( "c", 2 ) );

    JsonObject configuration = new ConfigLoader( project ).getConfiguration();

    assertEquals( 1, configuration.get( "a" ).asInt() );
    assertEquals( 1, configuration.get( "b" ).asInt() );
    assertNull( configuration.get( "c" ) );
  }

  @Test
  public void ignoresWorkspaceOptions_ifProjectSpecific() {
    workspacePrefs.setOptions( "a: 1, b: 1" );
    createProjectConfig( new JsonObject().add( "b", 2 ).add( "c", 2 ) );
    projectPrefs.setProjectSpecific( true );

    JsonObject configuration = new ConfigLoader( project ).getConfiguration();

    assertNull( configuration.get( "a" ) );
    assertEquals( 2, configuration.get( "b" ).asInt() );
    assertEquals( 2, configuration.get( "c" ).asInt() );
  }

  @Test
  public void fallsBackToOldProjectProperties_ifConfigFileMissing() {
    projectPrefs.setProjectSpecific( true );
    projectPrefs.setOptions( "a: 1" );
    projectPrefs.setGlobals( "foo: true" );

    JsonObject configuration = new ConfigLoader( project ).getConfiguration();

    assertEquals( 1, configuration.get( "a" ).asInt() );
    assertTrue( configuration.get( "predef" ).asObject().get( "foo" ).asBoolean() );
  }

  @Test
  public void ignoresOldProjectProperties_ifConfigFilePresent() {
    projectPrefs.setProjectSpecific( true );
    projectPrefs.setOptions( "a: 1, b: 1" );
    createProjectConfig( new JsonObject().add( "b", 2 ).add( "c", 2 ) );

    JsonObject configuration = new ConfigLoader( project ).getConfiguration();

    assertNull( configuration.get( "a" ) );
    assertEquals( 2, configuration.get( "b" ).asInt() );
    assertEquals( 2, configuration.get( "c" ).asInt() );
  }

  @Test
  public void emptyConfigForProjectsWithoutConfigFileAndProperties() {
    projectPrefs.setProjectSpecific( true );

    JsonObject configuration = new ConfigLoader( project ).getConfiguration();

    assertEquals( new JsonObject(), configuration );
  }

  private void createProjectConfig( JsonObject projectConfig ) {
    TestUtil.createFile( project, ".jshintrc", projectConfig.toString() );
  }

}
