package com.playmonumenta.redissync;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.playmonumenta.redissync.event.PlayerServerTransferEvent;

import io.github.jorelali.commandapi.api.CommandAPI;

public class MonumentaRedisSyncAPI {
	public static void sendPlayer(Plugin plugin, Player player, String target) throws Exception {
		if (MonumentaRedisSync.getInstance() == null) {
			throw new Exception("MonumentaRedisSync is not loaded!");
		}

		if (target.equalsIgnoreCase(Conf.getShard())) {
			player.sendMessage(ChatColor.RED + "Can not transfer to the same server you are already on");
			return;
		}

		/* TODO: Something to check if the player is already transferring */

		PlayerServerTransferEvent event = new PlayerServerTransferEvent(player, target);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return;
		}

		player.sendMessage(ChatColor.GOLD + "Transferring you to " + target);

		/* TODO: Lock player during transfer */

		try {
			MonumentaRedisSync.getInstance().getVersionAdapter().savePlayer(player);
		} catch (Exception ex) {
			String message = "Failed to save player data for player '" + player.getName() + "'";
			MonumentaRedisSync.getInstance().getLogger().severe(message);
			ex.printStackTrace();
			CommandAPI.fail(message);
		}

		/* Disable saving the player data again when they log out */
		DataEventListener.setPlayerAsTransferring(player);

		/*
		 * Use plugin messages to tell bungee to transfer the player.
		 * This is nice because in the event of multiple bungeecord's,
		 * it'll use the one the player is connected to.
		 */
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Connect");
		out.writeUTF(target);

		player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());

		/* TODO: Timeout if it fails and unlock. Remember to reinstate data saving! */
	}
}

