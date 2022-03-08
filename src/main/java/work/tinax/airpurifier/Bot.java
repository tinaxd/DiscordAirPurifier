package work.tinax.airpurifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Bot extends ListenerAdapter {
	private HashMap<String, GuildManager> guilds = new HashMap<>();
	private AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
	
	private final Path audioDirectory;
	private final HashMap<String, Path> availableAudios;
	
	public Bot(Path audioDirectory) throws IOException {
		this.audioDirectory = audioDirectory;
		this.availableAudios = new HashMap<>();
		buildAudioMap();
		System.out.println("loaded audio files:");
		availableAudios.forEach((id, p) -> System.out.println(id + ": " + p));
		AudioSourceManagers.registerLocalSource(playerManager);
	}
	
	private void buildAudioMap() throws IOException {
		Files.walk(audioDirectory).forEach(p -> {
			var f = p.toFile();
			if (f.isFile()) {
				var nameWithoutExt = FilenameUtils.getBaseName(f.getPath());
				availableAudios.put(nameWithoutExt, p);
			}
		});
	}
	
	private GuildManager getOrCreateGuildManager(String guildId) {
		var guild = guilds.get(guildId);
		if (guild != null) return guild;
		
		guild = new GuildManager(playerManager.createPlayer(), this);
		guilds.put(guildId, guild);
		return guild;
	}
	
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		getOrCreateGuildManager(event.getGuild().getId()).onSlashCommandInteraction(event);
	}
	
	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		getOrCreateGuildManager(event.getGuild().getId()).onButtonInteraction(event);
	}
	
	public boolean loadAudio(String identifier, TrackScheduler sched) {
		var path = availableAudios.get(identifier);
		if (path == null) {
			System.err.println("cannot get path of audio id " + identifier);
			return false;
		}
		playerManager.loadItem(path.toAbsolutePath().toString(), new AudioLoadResultHandler() {
			
			@Override
			public void trackLoaded(AudioTrack track) {
				sched.playNow(track);
			}
			
			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				System.err.println("playlist is not supported");
			}
			
			@Override
			public void noMatches() {
				System.err.println("no match");
			}
			
			@Override
			public void loadFailed(FriendlyException exception) {
				exception.printStackTrace();
			}
		});
		return true;
	}
	
	public List<String> getAudioIdentifiers() {
		return availableAudios.keySet().stream().sorted().toList();
	}
}
