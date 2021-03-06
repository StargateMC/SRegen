/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.draksterau.Regenerator.commands;

import com.draksterau.Regenerator.Handlers.MsgType;
import com.draksterau.Regenerator.Handlers.RChunk;
import com.draksterau.Regenerator.event.RegenerationRequestEvent;
import com.draksterau.Regenerator.event.RequestTrigger;
import com.draksterau.Regenerator.integration.Integration;
import com.draksterau.Regenerator.tasks.ChunkTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

/**
 *
 * @author draks
 */
public class regenCommand {
    
    RegeneratorCommand command;
    
    public regenCommand(RegeneratorCommand RegeneratorCommand) {
        this.command = RegeneratorCommand;
    }
    
    public void doCommand() {
        if (command.plugin.utils.getSenderPlayer(command.sender) == null) {
            command.sender.sendMessage(ChatColor.RED + "This command can only be performed while in-game.");
        } else {
            if (command.plugin.utils.isLagOK()) {
                Chunk rootChunk = command.plugin.utils.getSenderPlayer(command.sender).getLocation().getChunk();
                RChunk rChunk = new RChunk(command.plugin, rootChunk.getX(), rootChunk.getZ(), rootChunk.getWorld().getName());                        
                Player player = command.plugin.utils.getSenderPlayer(command.sender);
                RegenerationRequestEvent requestEvent = new RegenerationRequestEvent(player.getLocation(), player, RequestTrigger.Command, command.plugin);
                Bukkit.getServer().getPluginManager().callEvent(requestEvent);
                if (!requestEvent.isCancelled()) {
                    if (command.plugin.utils.canManuallyRegen(command.plugin.utils.getSenderPlayer(command.sender), rootChunk)) {
                        try {
                            new ChunkTask(rChunk, true).runTask(command.plugin);
                        } catch (Exception e) {
                            command.plugin.utils.throwMessage(MsgType.SEVERE, "Failed to regenerate chunk : " + rChunk.getChunk().getX() + "," + rChunk.getChunk().getZ() + " on world: " + rChunk.getWorldName());
                            if (command.plugin.config.debugMode) e.printStackTrace();
                        }
                        rChunk.resetActivity();
                        Integration integration = command.plugin.utils.getIntegrationForChunk(player.getLocation().getChunk());
                        if (integration != null && integration.isChunkClaimed(rootChunk)) {                    
                            player.sendMessage(command.plugin.utils.getFancyName() + integration.getPlayerRegenReason(player, rootChunk));  
                        } else {
                            player.sendMessage(command.plugin.utils.getFancyName() + ChatColor.GREEN + "The unclaimed area around you has been regenerated.");
                        }
                    } else {
                        Chunk senderChunk = command.plugin.utils.getSenderChunk(command.sender);
                        if (!rChunk.canManualRegen()) {
                            player.sendMessage(command.plugin.utils.getFancyName() + ChatColor.RED + "Failed to perform manual regeneration as the world you are on has it disabled.");
                        } else {
                            if (command.plugin.utils.getCountIntegration(player.getLocation().getChunk()) == 1) {
                                player.sendMessage(command.plugin.utils.getFancyName() + command.plugin.utils.getIntegrationForChunk(senderChunk).getPlayerRegenReason(player, rootChunk));
                                player.sendMessage(command.plugin.utils.getFancyName() + "This requires the permission node: " + command.plugin.utils.getIntegrationForChunk(senderChunk).getPermissionRequiredToRegen(player, rootChunk));
                            } else {
                                if (command.plugin.utils.getCountIntegration(player.getLocation().getChunk()) > 1) {
                                    player.sendMessage(command.plugin.utils.getFancyName() + ChatColor.RED + "This chunk is claimed by more than one grief prevention plugin. It can only be regenerated by OPS or those with the regenerator.regen.override permission node.");
                                } else {
                                    player.sendMessage(command.plugin.utils.getFancyName() + ChatColor.RED + "This chunk is unclaimed and requires the regenerator.regen.unclaimed permission node to regenerate.");
                                }
                            }
                        }
                    }
                } else {                        
                    player.sendMessage(command.plugin.utils.getFancyName() + ChatColor.RED + "Regeneration Request denied due to the following:");
                    for (String s : requestEvent.getCancelledReasons().keySet()) {
                        player.sendMessage(command.plugin.utils.getFancyName() + ChatColor.RED + s + " provided by : " + requestEvent.getCancelledReasons().get(s).getName() + ".");
                    }
                }
            } else {
                command.plugin.utils.getSenderPlayer(command.sender).sendMessage(command.plugin.utils.getFancyName() + ChatColor.RED + "Regeneration capabilities have been suspended - TPS has dropped below what is set in configuration.");
            }

        }
    }
}
