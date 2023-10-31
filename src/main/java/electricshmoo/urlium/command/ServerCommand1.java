package electricshmoo.urlium.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import electricshmoo.urlium.UrlComMod;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ServerCommand1 {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess commandRegistryAccess,
                                CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(
                (LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal(UrlComMod.getMessageCommand1Name())
                    .requires(source -> source.hasPermissionLevel( UrlComMod.messageCommand1Level )))
                    .then(
                        CommandManager.argument("message", MessageArgumentType.message())
                            .executes(context -> {
                                MessageArgumentType.getSignedMessage(context, "message", message -> {
                                    PlayerEntity player = context.getSource().getPlayer();
                                    Map<Object, Object> data = new HashMap<>();
                                    long unixTime = System.currentTimeMillis();
                                    data.put("ts", unixTime);
                                    data.put("device", "message");
                                    data.put("message", message.getContent().getString());
                                    data.put("user", player.getEntityName());

                                    try {
                                        UrlComMod.sendPOST(data);
                                    } catch (IOException ignored) {}

                                });
                                return 1;
                            })
                    )
        );
    }
}
