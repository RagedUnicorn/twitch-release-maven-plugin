package com.ragedunicorn.tools.maven;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;

public class TwitchReleaseMojoTest extends AbstractMojoTestCase {

  public void setUp() throws Exception {
    super.setUp();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Tests the proper discovery and configuration of the mojo.
   *
   * @throws Exception If failing to extract plugin configuration
   */
  public void testBasicPluginConfiguration() throws Exception {

    File testPom = new File("src/test/resources/plugin-config.xml" );
    assertNotNull(testPom);
    assertTrue(testPom.exists());

    TwitchReleaseMojo mojo = new TwitchReleaseMojo();
    mojo = (TwitchReleaseMojo) configureMojo(
        mojo, extractPluginConfiguration("twitch-release-maven-plugin", testPom)
    );

    assertNotNull(mojo);

    // mojo.execute(); execution requires twitch backend
  }
}
