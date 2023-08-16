package electricshmoo.urlium.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
public class QueryCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess commandRegistryAccess,
                                CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(
                CommandManager.literal("ugetblock")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> execute(context, BlockPosArgumentType.getLoadedBlockPos(context, "pos")))
                        )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context, BlockPos pos) {
        ServerCommandSource source = context.getSource();
        BlockState blockState = source.getWorld().getBlockState(pos);
        ServerWorld world = context.getSource().getWorld();
        StringBuilder stringBuilder = new StringBuilder(BlockArgumentParser.stringifyBlockState(blockState));
        if (blockState.hasBlockEntity()) {
            BlockEntity blockEntity =  world.getBlockEntity(pos);
            if (blockEntity != null) {
                NbtCompound nbtCompound = blockEntity != null ? blockEntity.createNbt() : null;
                if (nbtCompound != null) {
                    stringBuilder.append(nbtCompound);
                }
            }
        }
        source.sendFeedback(() -> Text.literal( stringBuilder.toString()), false);
        return 1;
    }

}
