package electricshmoo.urlium.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import electricshmoo.urlium.util.IItemDataSaver;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class BookCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess commandRegistryAccess,
                                CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(
                CommandManager.literal("urliumbook").executes(context -> execute(context))
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        ItemStack heldStack = getMainHandItems( player );
        Item heldItem  = heldStack.getItem();

        IItemDataSaver heldItemData = ((IItemDataSaver) heldItem);

        var pdata = heldItemData.getPersistentData();

        if (pdata.contains("report")) {
            player.sendMessage(Text.literal("Change reporting on this book disabled.  ID: "+pdata.getString("report")),false);
            pdata.remove("report");
        } else {
            NbtCompound nbt = new NbtCompound();
            long unixTime = System.currentTimeMillis();
            String id = "book_"+unixTime;
            nbt.putString("report", id);
            player.sendMessage(Text.literal("Change reporting on this book enabled.  ID: "+id),false);
        }
        return 1;
    }

    private static ItemStack getMainHandItems(ServerPlayerEntity player) {
        Iterable<ItemStack> handItems = player.getHandItems();

        for (ItemStack itemStack : handItems) {
            if (itemStack != null && itemStack != ItemStack.EMPTY) {
                if (player.getMainHandStack().equals(itemStack)) {
                    return itemStack;
                }
            }
        }

        return ItemStack.EMPTY; // No main hand-held item found
    }

}
