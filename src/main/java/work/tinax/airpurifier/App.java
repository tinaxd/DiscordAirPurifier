package work.tinax.airpurifier;

import java.io.IOException;
import java.nio.file.Paths;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class App {
	public static void main(String[] args) throws LoginException, InterruptedException, IOException {
		String token = System.getenv("DISCORD_TOKEN");
		if (token == null || token.isEmpty()) {
			System.err.println("DISCORD_TOKEN is not set");
			System.exit(1);
		}
		String audioDirectory = System.getenv("AUDIO_DIRECTORY");
		if (audioDirectory == null || audioDirectory.isEmpty()) {
			System.err.println("AUDIO_DIRECTORY is not set");
			System.exit(1);
		}
		
		JDA jda = JDABuilder.createDefault(token).build();
		jda.awaitReady();
		jda.upsertCommand("kuki", "Turn on the IoT-enabled air purifier!").queue();
		/* for testing
		jda
			.getGuildById(<insert your guild id here>)
			.upsertCommand("kuki", "Turn on the IoT-enabled air purifier!")
			.queue();
		*/
		jda.addEventListener(new Bot(Paths.get(audioDirectory)));
	}
}
