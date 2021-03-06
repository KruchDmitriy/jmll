package com.expleague.crawl;

import com.expleague.crawl.sessions.WeightedRandomWalkGameSession;
import com.expleague.commons.system.RuntimeUtils;

import javax.websocket.DeploymentException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

/**
 * Experts League
 * Created by solar on 23/03/16.
 */
public class StartCrawl {
  private static final Logger log = Logger.getLogger(StartCrawl.class.getName());
  public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException, DeploymentException {
    final File socketFile = File.createTempFile("crawl", ".socket");
    final RuntimeUtils.BashProcess bash = new RuntimeUtils.BashProcess("experiments/crawl", false);
    try {
      //noinspection ResultOfMethodCallIgnored
      socketFile.delete();
      bash.exec("bash ./run_server.sh");
      Thread.sleep(1000);
      final WSEndpoint endpoint = new WSEndpoint(new URI("ws://localhost:8080/socket"));
      int index = 0;
      while(true) {
        final WeightedRandomWalkGameSession session = new WeightedRandomWalkGameSession(new File("session-" + (index++) + ".txt"));
        final GameProcess gameProcess = new GameProcess(endpoint, session);
        gameProcess.run();
        log.info("Session finished with score: " + gameProcess.score());
        session.alter(gameProcess.score());
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      bash.destroy();
    }
    finally {
      bash.waitFor();
      //noinspection ResultOfMethodCallIgnored
      socketFile.delete();
    }
  }
}
