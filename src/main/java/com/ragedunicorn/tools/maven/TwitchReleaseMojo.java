/*
 * Copyright (c) 2020 Michael Wiesendanger
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:

 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.ragedunicorn.tools.maven;

import com.ragedunicorn.tools.maven.model.Metadata;
import com.ragedunicorn.tools.maven.service.ReleaseService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;


@Mojo(name = "twitch-release")
public class TwitchReleaseMojo extends AbstractMojo {
  // Type of game (currently only wow is supported)
  @Parameter(property = "game", defaultValue = "wow")
  private String game;

  // The project id of the twitch project (can be found on the projects page)
  @Parameter(property = "projectId", required = true)
  private String projectId;

  // An optional displayname for the uploaded file
  @Parameter(property = "displayName", defaultValue = "addon")
  private String displayName;

  // A string containing the changelog
  @Parameter(property = "changelog")
  private String changelog;

  // Optional path to a changelog file - will override changelog
  @Parameter(property = "changelogFile")
  private String changelogFile;

  // Changelog type ["text", "html", "markdown"]
  @Parameter(property = "changelogType", defaultValue = "text")
  private String changelogType;

  // A list of supported game versions
  @Parameter(property = "gameVersions", required = true)
  private String[] gameVersions;

  // One of "alpha", "beta", "release"
  @Parameter(property = "releaseType", defaultValue = "release")
  private String releaseType;

  // The path to the addon to upload
  @Parameter(property = "file", required = true)
  private String file;

  // Alternative of using a server configuration. The token can directly be placed in the
  // plugin configuration
  @Parameter(property = "token")
  private String token;

  // References a server configuration in your .m2 settings.xml. This is the preferred way for
  // using the generated twitch token
  @Parameter(property = "server")
  private String server;

  @Parameter(defaultValue = "${settings}", readonly = true)
  private Settings settings;

  /**
   * Plugin execution callback.
   *
   * @throws MojoExecutionException If any exception happens during the execution of the plugin
   */
  public void execute() throws MojoExecutionException {
    validateRequiredInputParameters();

    TwitchClient twitchClient = createTwitchClient();
    createRelease(twitchClient);
  }

  /**
   * Create a new release on Twitch.
   *
   * @param twitchClient The Twitch client
   *
   * @throws MojoExecutionException If any exception happens during the execution of the release
   *                                service
   */
  private void createRelease(TwitchClient twitchClient) throws MojoExecutionException {
    final ReleaseService releaseService = new ReleaseService(twitchClient);
    Metadata metadata = new Metadata();

    metadata.setChangelog(getChangelog());
    metadata.setChangelogType(changelogType);
    metadata.setDisplayName(displayName);

    int[] convertedGameVersions = Arrays.stream(gameVersions).mapToInt(Integer::parseInt).toArray();
    metadata.setGameVersions(convertedGameVersions);
    metadata.setReleaseType(releaseType);

    releaseService.createReleaseOperation(metadata, file);
  }

  /**
   * Retrieve the token for the Twitch Api.
   *
   * @return A property object containing the Twitch Api token
   *
   * @throws MojoExecutionException An exception occurring during the execution of a plugin
   */
  private String getCredentials() throws MojoExecutionException {
    // prefer settings parameter over direct configuration in pom
    if (settings != null && server != null) {
      final Server serverEntry = settings.getServer(server);
      if (serverEntry != null) {
        token = serverEntry.getPassphrase();

        if (token == null || token.isEmpty()) {
          throw new MojoExecutionException("Found server entry in settings.xml "
              + "but authToken parameter was missing or is empty");
        }
      } else {
        getLog().warn("Unable to retrieve settings or server. Falling back to project settings");
      }
    }
    // fallback to plugin configuration if credentials cannot be retrieved from maven settings.xml
    if (token == null) {
      throw new MojoExecutionException("Unable to read authentication configuration make "
          + "sure to set the authToken property");
    }

    return token;
  }

  private String getChangelog() throws MojoExecutionException {
    if (!changelogFile.isEmpty()) {
      byte[] changelogContent;

      try {
        Path changelogPath = Paths.get(changelogFile);
        if (getLog().isDebugEnabled()) {
          getLog().debug("Changelog path: " + changelogFile);
        }
        changelogContent = Files.readAllBytes(changelogPath);

        return new String(changelogContent, StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new MojoExecutionException("Failed to read release notes", e);
      }
    } else if (!changelog.isEmpty()) {
      return changelog;
    }

    return "";
  }

  /**
   * Validate required input parameters.
   *
   * @throws MojoExecutionException An exception occurring during the execution of a plugin
   */
  private void validateRequiredInputParameters() throws MojoExecutionException {
    if (projectId == null || projectId.isEmpty()) {
      throw new MojoExecutionException("Missing required parameter projectId");
    }

    if (gameVersions == null || gameVersions.length <= 0) {
      throw new MojoExecutionException("Missing required parameter gameVersions");
    }

    if (file == null || file.isEmpty()) {
      throw new MojoExecutionException("Missing required parameter file");
    }
  }

  /**
   * Create a new Twitch client and set its owner and the targeted repository.
   * Additionally the token for authenticating against the Twitch Api is set.
   *
   * @return The created Twitch client
   *
   * @throws MojoExecutionException An exception occurring during the execution of a plugin
   */
  private TwitchClient createTwitchClient() throws MojoExecutionException {
    TwitchClient twitchClient = new TwitchClient();
    twitchClient.setToken(getCredentials());
    twitchClient.setGame(game);
    twitchClient.setProjectId(projectId);

    return twitchClient;
  }
}
