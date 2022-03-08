package work.tinax.airpurifier;

import java.util.List;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.AudioManager;

public class GuildManager {
	private final AudioPlayer player;
	private final TrackScheduler track;
	private final AudioPlayerSendHandler sendHandler;
	private final Bot bot;
	private Guild guild;
	
	private boolean inVoiceChannel = false;
	
	public GuildManager(AudioPlayer player, Bot bot) {
		this.player = player;
		track = new TrackScheduler(player, () -> {
			if (guild != null) { // TODO: もっと確実な方法を
				leaveVoiceChannel(guild.getAudioManager());
			}
		});
		player.addListener(track);
		sendHandler = new AudioPlayerSendHandler(player);
		this.bot = bot;
	}
	
	private List<Button> generateButtons() {
		return bot.getAudioIdentifiers().stream().map(id -> {
			return Button.primary(id, id);
		}).toList();
	}
	
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		guild = event.getGuild();
		if (event.getName().strip().equals("kuki")) {
			event
				.reply("音を選んでください")
				.addActionRow(generateButtons())
				.queue();
		}
	}
	
	public void onButtonInteraction(ButtonInteractionEvent event) {
		var id = event.getComponentId();
//		event.reply("id " + id + " clicked").queue();
		var member = event.getMember();
		if (member == null) return;
		var guild = event.getGuild();
		this.guild = guild;
		if (guild == null) return;
		
		var vcChan = guild
			.getVoiceChannels()
			.stream()
			.filter(c -> c.getMembers().contains(member))
			.findFirst();
		if (vcChan.isPresent()) {
			if (inVoiceChannel) {
				event.reply("現在の再生が終わるまでお待ちください").queue();
				return;
			}
			joinVoiceChannel(guild.getAudioManager(), vcChan.get());
			if (!bot.loadAudio(id, track)) {
				// failed to find audio
				event.reply("エラー: 音声ファイルが見つかりません").queue();
			} else {
				event.reply("再生します").queue();
			}
		} else {
			event.reply("VCに入ってから呼んでください").queue();
		}
	}
	
	public void joinVoiceChannel(AudioManager am, VoiceChannel vc) {
		am.setSendingHandler(sendHandler);
		am.openAudioConnection(vc);
		inVoiceChannel = true;
	}
	
	public void leaveVoiceChannel(AudioManager am) {
		am.closeAudioConnection();
		inVoiceChannel = false;
	}
}
