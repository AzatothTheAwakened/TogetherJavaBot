package org.togetherjava;

import com.moandjiezana.toml.Toml;
import java.util.Objects;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Message;
import org.togetherjava.command.CommandListener;
import org.togetherjava.messaging.BotMessage;
import org.togetherjava.messaging.ComplexMessage;
import org.togetherjava.messaging.SimpleMessage;
import org.togetherjava.messaging.sending.DestructingMessageSender;
import org.togetherjava.messaging.transforming.CategoryColorTransformer;
import org.togetherjava.messaging.transforming.EmbedTransformer;
import org.togetherjava.messaging.transforming.Transformer;
import org.togetherjava.reactions.ReactionListener;
import org.togetherjava.storage.sql.Database;
import org.togetherjava.util.Context;

public class TogetherJavaBot {

  private Toml config;
  private JDA jda;

  public TogetherJavaBot(Toml config) {
    this.config = Objects.requireNonNull(config, "config can not be null!");
  }

  /**
   * Starts the bot and waits for it to be ready.
   *
   * @throws InterruptedException if an error occurs while waiting
   * @throws LoginException if the token is invalid or another error prevents login
   */
  public void start(String token) throws InterruptedException, LoginException {
    Transformer<BotMessage, BotMessage> simpleMessageTransformer = Transformer
        .defaultTypeSwitch(
            SimpleMessage.class,
            new EmbedTransformer(),
            message -> message
        );

    Transformer<BotMessage, BotMessage> embedColorTransformer = Transformer
        .defaultTypeSwitch(
            ComplexMessage.class,
            new CategoryColorTransformer(),
            it -> it
        );

    Transformer<BotMessage, Message> toMessageTransformer = message -> message.toDiscordMessage()
        .build();

    DestructingMessageSender messageSender = new DestructingMessageSender(
        config,
        simpleMessageTransformer.then(embedColorTransformer).then(toMessageTransformer)
    );

    ReactionListener reactionListener = new ReactionListener();
    CommandListener commandListener = new CommandListener(
        config.getList("commands.prefixes"),
        config
    );
    Database database = new Database(config.getString("database.connection-url"));

    Context context = new Context(messageSender, reactionListener, commandListener, config,
        database);

    commandListener.setContext(context);
    reactionListener.setContext(context);

    jda = new JDABuilder(AccountType.BOT)
        .setToken(token)
        .addEventListener(commandListener)
        .addEventListener(reactionListener)
        .build()
        .awaitReady();
  }

  /**
   * Stops this bot, after all actions were run.
   */
  public void stop() {
    jda.shutdown();
  }
}
