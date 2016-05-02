package com.spbsu.crawl;

import com.spbsu.commons.random.FastRandom;
import com.spbsu.commons.util.ThreadTools;
import com.spbsu.crawl.bl.Mob;
import com.spbsu.crawl.bl.map.CrawlSystemMap;
import com.spbsu.crawl.data.Command;
import com.spbsu.crawl.data.GameSession;
import com.spbsu.crawl.data.Hero;
import com.spbsu.crawl.data.Message;
import com.spbsu.crawl.data.impl.*;
import com.spbsu.crawl.data.impl.system.*;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * User: qdeee
 * Date: 03.04.16
 */
public class GameProcess implements Runnable {
  private static final Logger LOG = Logger.getLogger(GameProcess.class.getName());

  private final WSEndpoint endpoint;
  private final GameSession session;
  private CrawlSystemMap crawlSystemMap = new CrawlSystemMap();

  public GameProcess(WSEndpoint endpoint, GameSession session) {
    this.endpoint = endpoint;
    this.session = session;
  }

  private final FastRandom rng = new FastRandom();
  public void run() {
    skipTo(LobbyComplete.class);
    final String user = rng.nextLowerCaseString(10);
    endpoint.send(new RegisterMessage(user, user, user + "@localhost"));
    skipTo(SetGameLinks.class);
    endpoint.send(new StartGameMessage("dcss-web-trunk"));
    skipTo(PlayerInfoMessage.class);
    endpoint.send(new InputCommandMessage(session.race().select()));
    final Hero.Spec spec = session.spec();
    endpoint.send(new InputCommandMessage(spec.selectProf()));
    if (spec.hasSpec())
      endpoint.send(new InputCommandMessage(spec.selectSpec()));
    Message message;

    crawlSystemMap.subscribeToEvents(session::updateMap);
    do {
      message = endpoint.poll();
      if (message instanceof Command) {
        ((Command) message).execute(endpoint.out);
      } else if (message instanceof UpdateMapMessage) {
        crawlSystemMap.updater().message((UpdateMapMessage) message);
      } else if (message instanceof PlayerInfoMessage) {
        crawlSystemMap.updater().message((PlayerInfoMessage) message);
      } else if (message instanceof InputModeMessage) {
        switch (((InputModeMessage) message).inputMode()) {
          case 0:
            break;
          case 1:
            final Mob.Action tickAction = session.tick();
            endpoint.send(new InputCommandMessage(tickAction.code()));
            break;
          case 5:
            endpoint.send(new InputCommandMessage(' '));
            break;

          default:
            LOG.warning("Unknown input mode received: " + message.json());
        }
      }
    }
    while (true);
  }

  private <T extends Message> T skipTo(Class<T> aClass) {
    Message message;
    do {
      message = endpoint.poll();
      if (message instanceof Command) {
        ((Command) message).execute(endpoint.out);
      }
      else {
        LOG.info("Skipped message: " + message.toString());
      }
    }
    while (!aClass.isAssignableFrom(message.getClass()));
    //noinspection unchecked
    return (T)message;
  }
}
