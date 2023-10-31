package electricshmoo.urlium.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import electricshmoo.urlium.UrlComMod;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Clearable;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class USetBlockCommand {

        public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess commandRegistryAccess,
                                CommandManager.RegistrationEnvironment registrationEnvironment) {
            /*
            dispatcher.register(
                (LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("usetblock").requires(source -> source.hasPermissionLevel(UrlComMod.usetblocklevel)))
                    .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                        .then((ArgumentBuilder<ServerCommandSource, ?>)((RequiredArgumentBuilder)((RequiredArgumentBuilder)(
                                (RequiredArgumentBuilder)CommandManager.argument("block", BlockStateArgumentType.blockState(commandRegistryAccess)).executes(context -> USetBlockCommand.execute(context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"), BlockStateArgumentType.getBlockState(context, "block"), USetBlockCommand.Mode.REPLACE, null))
                                    ).then(CommandManager.literal("destroy").executes(context -> USetBlockCommand.execute(context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"), BlockStateArgumentType.getBlockState(context, "block"), USetBlockCommand.Mode.DESTROY, null)))
                                ).then(CommandManager.literal("keep").executes(context ->USetBlockCommand.execute(context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"), BlockStateArgumentType.getBlockState(context, "block"), USetBlockCommand.Mode.REPLACE, pos -> pos.getWorld().isAir(pos.getBlockPos()))))
                            ).then(CommandManager.literal("replace").executes(context -> USetBlockCommand.execute(context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"), BlockStateArgumentType.getBlockState(context, "block"), USetBlockCommand.Mode.REPLACE, null)))
                        )
                    )
            );

                                .executes(context -> execute(context, BlockPosArgumentType.getBlockPos(context, "pos"), DimensionArgumentType.getDimensionArgument(context, "world")))

            */
            dispatcher.register(
                    (LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("usetblock").requires(source -> source.hasPermissionLevel(UrlComMod.usetblocklevel))
                            .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                    .then(CommandManager.argument("world", DimensionArgumentType.dimension())  // Capture the dimension argument
                                            .then((ArgumentBuilder<ServerCommandSource, ?>)((RequiredArgumentBuilder)((RequiredArgumentBuilder)(
                                                            (RequiredArgumentBuilder)CommandManager.argument("block", BlockStateArgumentType.blockState(commandRegistryAccess)).executes(context -> USetBlockCommand.execute(context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"), BlockStateArgumentType.getBlockState(context, "block"), USetBlockCommand.Mode.REPLACE, null, DimensionArgumentType.getDimensionArgument(context, "world"))) // Pass the dimension argument
                                                    ).then(CommandManager.literal("destroy").executes(context -> USetBlockCommand.execute(context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"), BlockStateArgumentType.getBlockState(context, "block"), USetBlockCommand.Mode.DESTROY, null, DimensionArgumentType.getDimensionArgument(context, "world"))) // Pass the dimension argument
                                                    ).then(CommandManager.literal("keep").executes(context -> USetBlockCommand.execute(context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"), BlockStateArgumentType.getBlockState(context, "block"), USetBlockCommand.Mode.REPLACE, pos -> pos.getWorld().isAir(pos.getBlockPos()), DimensionArgumentType.getDimensionArgument(context, "world"))) // Pass the dimension argument
                                                    ).then(CommandManager.literal("replace").executes(context -> USetBlockCommand.execute(context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"), BlockStateArgumentType.getBlockState(context, "block"), USetBlockCommand.Mode.REPLACE, null, DimensionArgumentType.getDimensionArgument(context, "world"))) // Pass the dimension argument
                                                        )
                                                    )
                                                )
                                            )
                                    )
                            )
                    )
            );
    }

    private static int execute(ServerCommandSource source, BlockPos pos, BlockStateArgument block, USetBlockCommand.Mode mode, @Nullable Predicate<CachedBlockPosition> condition, ServerWorld world){
        boolean bl;
        //ServerWorld serverWorld = source.getWorld();
        if (mode == USetBlockCommand.Mode.DESTROY) {
            world.breakBlock(pos, true);
            bl = !block.getBlockState().isAir() || !world.getBlockState(pos).isAir();
        } else {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            Clearable.clear(blockEntity);
            bl = true;
        }
        block.setBlockState(world, pos, Block.NOTIFY_LISTENERS);
        world.updateNeighbors(pos, block.getBlockState().getBlock());
        return 1;
    }

    public enum Mode {
        REPLACE,
        DESTROY;
    }

    public static interface Filter {
        @Nullable
        BlockStateArgument filter(BlockBox var1, BlockPos var2, BlockStateArgument var3, ServerWorld var4);
    }
}
