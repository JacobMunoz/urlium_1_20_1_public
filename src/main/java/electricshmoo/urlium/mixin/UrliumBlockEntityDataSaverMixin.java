package electricshmoo.urlium.mixin;

import com.google.gson.Gson;
import electricshmoo.urlium.UrlComMod;
import electricshmoo.urlium.util.IBlockEntityDataSaver;
import net.minecraft.block.*;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static net.minecraft.block.ChestBlock.getInventory;

@Mixin(BlockEntity.class)
public abstract class UrliumBlockEntityDataSaverMixin implements IBlockEntityDataSaver {
    @Shadow @Nullable protected World world;
    private NbtCompound persistData;
    @Override
    public NbtCompound getPersistentData() {
        if (this.persistData == null) {
            this.persistData = new NbtCompound();
        }
        return persistData;
    }

    @Inject(method="readNbt", at=@At("HEAD"))
    protected  void injectReadMethod(NbtCompound nbt, CallbackInfo info){
        if (nbt.contains("urlium",10))
            this.persistData = nbt.getCompound("urlium");
    }

    @Inject(method="writeNbt", at=@At("HEAD"))
    protected  void injectWriteMethod(NbtCompound nbt, CallbackInfo info){
        if (this.persistData != null) {
            nbt.put("urlium",persistData);
            if (persistData.contains("report")) {

                int[] coords = persistData.getIntArray("report");
                BlockPos blockPos = new BlockPos(coords[0],coords[1],coords[2] );
                BlockState blockState = world.getBlockState(blockPos);

                StringBuilder blockStateStringBuilder = new StringBuilder(BlockArgumentParser.stringifyBlockState(blockState));
                String blockStateString = blockStateStringBuilder.toString();

                StringBuilder result = new StringBuilder();
                long unixTime = System.currentTimeMillis();
                Map<Object, Object> data = new HashMap<>();
                String blockType = persistData.getString("blocktype");

                if (blockType.equals("chest")) {
                    ChestBlock block = (ChestBlock) blockState.getBlock();
                    Inventory inv  = getInventory(block, blockState, world, blockPos, true);
                    Gson gson = new Gson();
                    result.append("{");
                    for (int slot = 0; slot < inv.size(); slot++) {
                        ItemStack stack = inv.getStack(slot);
                        if (!stack.isEmpty()) {
                            if (slot > 0) result.append(",");
                            result.append("\"slot\":"+Integer.toString( slot )+",");
                            result.append("\"count\":"+Integer.toString( stack.getCount() )+",");
                            result.append("\"item\":"+gson.toJson(stack.getItem().toString())+",");
                            result.append("\"name\":"+gson.toJson(stack.getName().getString()));
                        }
                    }
                    result.append("}");
                    data.put("device", "chest");
                    data.put("inventory", result.toString());
                } else if (blockType.equals("barrel")) {
                    BarrelBlockEntity barrelEntity = (BarrelBlockEntity) world.getBlockEntity(blockPos);
                    Gson gson = new Gson();
                    result.append("{");
                    for (int slot = 0; slot < barrelEntity.size(); slot++) {
                        ItemStack stack = barrelEntity.getStack(slot);
                        if (!stack.isEmpty()) {
                            if (slot > 0) result.append(",");
                            result.append("\"slot\":"+Integer.toString( slot )+",");
                            result.append("\"count\":"+Integer.toString( stack.getCount() )+",");
                            result.append("\"item\":"+gson.toJson(stack.getItem().toString())+",");
                            result.append("\"name\":"+gson.toJson(stack.getName().getString()));
                        }
                    }
                    result.append("}");
                    data.put("device", "barrel");
                    data.put("inventory", result.toString());
                } else if (blockType.equals("lectern")) {
                    LecternBlock block = (LecternBlock) blockState.getBlock();
                    LecternBlockEntity blockEntity = (LecternBlockEntity) world.getBlockEntity(blockPos);
                    data.put("device", "lectern");
                    if (blockEntity.hasBook()) {
                        ItemStack book = blockEntity.getBook();
                        NbtCompound bookNbt = book.getNbt();
                        result.append(bookNbt.toString());
                        data.put("bookContents", result.toString());
                    } else {
                        data.put("bookContents", "");
                    }
                } else if (blockType.indexOf("_sign")>0) {
                    SignBlock block = (SignBlock) blockState.getBlock();
                    SignBlockEntity blockEntity = (SignBlockEntity) world.getBlockEntity(blockPos);
                    data.put("device", persistData.getString("blocktype"));
                    Text[] frontTextMessages = blockEntity.getFrontText().getMessages(false);
                    String frontText = "[";
                    for (int i = 0; i < frontTextMessages.length; i++) {
                        frontText  +=  Text.Serializer.toJson(frontTextMessages[i]);; // Extract the text content as a plain string
                        if (i+1 < frontTextMessages.length) frontText += ",";
                        else frontText += "]";
                    }
                    data.put("frontText", frontText);

                    Text[] backTextMessages = blockEntity.getBackText().getMessages(false);
                    String backText = "[";
                    for (int i = 0; i < backTextMessages.length; i++) {
                        backText  +=  Text.Serializer.toJson(backTextMessages[i]);; // Extract the text content as a plain string
                        if (i+1 < backTextMessages.length) backText += ",";
                        else backText += "]";
                    }
                    data.put("backText", backText);

                } else {
                    return;
                }
                data.put("blockState", blockStateString);
                data.put("ts", unixTime);
                data.put("x", coords[0]);
                data.put("y", coords[1]);
                data.put("z", coords[2]);
                try {
                    UrlComMod.sendPOST(data);
                    UrlComMod.LOGGER.info("Reported on BlockEntity at: " + coords[0] + " "+coords[1]+" "+coords[2] );
                } catch (IOException ignore) {
                    UrlComMod.LOGGER.info("Failed to send post report on container at: " + coords[0] + " "+coords[1]+" "+coords[2] );
                }
            }
        }
    }
}
