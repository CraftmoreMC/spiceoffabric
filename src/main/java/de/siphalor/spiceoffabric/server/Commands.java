package de.siphalor.spiceoffabric.server;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.util.IHungerManager;
import de.siphalor.spiceoffabric.util.IServerPlayerEntity;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

import java.util.Collection;
import java.util.Collections;

public class Commands {
	public static void register() {
		CommandRegistrationCallback.EVENT.register((commandDispatcher, dedicated) -> {
			commandDispatcher.register(CommandManager.literal(SpiceOfFabric.MOD_ID + ":clearfoods")
					.requires(source -> source.hasPermissionLevel(2))
					.executes(context ->
							clearFoods(
									context.getSource(),
									Collections.singleton(context.getSource().getPlayer())
							)
					).then(
							CommandManager.argument("targets", EntityArgumentType.player())
							.executes(context ->
									clearFoods(
											context.getSource(),
											EntityArgumentType.getPlayers(context, "targets")
									)
							)
					)
			);
		});
	}

	private static int clearFoods(ServerCommandSource commandSource, Collection<ServerPlayerEntity> serverPlayerEntities) {
		for(ServerPlayerEntity serverPlayerEntity : serverPlayerEntities) {
			((IHungerManager) serverPlayerEntity.getHungerManager()).spiceOfFabric_clearHistory();
			if(((IServerPlayerEntity) serverPlayerEntity).spiceOfFabric_hasClientMod()) {
				ServerSidePacketRegistry.INSTANCE.sendToPlayer(serverPlayerEntity, SpiceOfFabric.CLEAR_FOODS_S2C_PACKET, new PacketByteBuf(Unpooled.buffer()));
				if(Config.carrot.enable)
					serverPlayerEntity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(Config.carrot.startHearts * 2);
				serverPlayerEntity.sendMessage(new TranslatableText("spiceoffabric.command.clearfoods.was_cleared"), false);
			} else {
				serverPlayerEntity.sendMessage(new LiteralText("Your food history has been cleared"), false);
			}
		}

        if(commandSource.getEntity() instanceof ServerPlayerEntity && (serverPlayerEntities.size() != 1 && serverPlayerEntities.iterator().next() == commandSource.getEntity())) {
        	if(((IServerPlayerEntity) commandSource.getEntity()).spiceOfFabric_hasClientMod()) {
        		commandSource.sendFeedback(new TranslatableText("spiceoffabric.command.clearfoods.cleared_players", serverPlayerEntities.size()), true);
			} else {
        		commandSource.sendFeedback(new LiteralText("Cleared food histories of " + serverPlayerEntities.size() + " players."), true);
			}
		}
		return serverPlayerEntities.size();
	}
}
