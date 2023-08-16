package electricshmoo.urlium.item;

import electricshmoo.urlium.UrlComMod;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.WritableBookItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class UrlPostBook extends WritableBookItem implements PolymerItem {

    private final PolymerModelData polymerModel;
    private static String postUrl;
    private static String userAgent;
    private NbtCompound bookNbt;
    public UrlPostBook(Settings settings, String modelId, String url, String agent) {
        super(settings);
        postUrl = url;
        userAgent = agent;
        this.polymerModel = PolymerResourcePackUtils.requestModel(Items.WRITABLE_BOOK, new Identifier("urlium", modelId));
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return this.polymerModel.item();
    }

    @Override
    public int getPolymerCustomModelData(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        if (itemStack.hasNbt()) {
            this.bookNbt = itemStack.getNbt();
            UrlComMod.LOGGER.info("GOT getNbt " + this.bookNbt.toString());
        }

        return this.polymerModel.value();
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        UrlComMod.LOGGER.info("Started editing book" );
        return super.use(world, user, hand);
    }
}