package com.ragedunicorn.tools.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;

public class TwitchReleaseTwitchClientTest {
  @Test
  public void testEndpointUriPreparation() {
    final String ENDPOINT = "/api/projects/:projectId/upload-file";
    final String projectId = "111111";
    final String game = "wow";

    TwitchClient client = new TwitchClient();
    client.setProjectId(projectId);
    client.setGame(game);
    client.setToken("test-token");


    try {
      URI preparedUri = client.prepareEndpointUri(ENDPOINT);
      Assert.assertEquals(preparedUri.toString(), "https://" + game + ".curseforge.com/api/projects/" + projectId + "/upload-file");
    } catch (MojoExecutionException e) {
      Assert.fail("MojoExecutionException: " + Arrays.toString(e.getStackTrace()));
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testPrepareEndpointUriExpectedInvalidState() {
    TwitchClient client = new TwitchClient();
    try {
      client.prepareEndpointUri("/some/url");
    } catch (MojoExecutionException e) {
      Assert.fail("MojoExecutionException: " + Arrays.toString(e.getStackTrace()));
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testGetClientUriExpectedInvalidState() {
    TwitchClient client = new TwitchClient();
    client.getHttpClient();
  }
}
