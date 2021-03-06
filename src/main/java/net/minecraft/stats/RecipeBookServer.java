package net.minecraft.stats;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ResourceKeyInvalidException;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.protocol.game.PacketPlayOutRecipes;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.item.crafting.CraftingManager;
import net.minecraft.world.item.crafting.IRecipe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.bukkit.craftbukkit.event.CraftEventFactory; // CraftBukkit

public class RecipeBookServer extends RecipeBook {

    public static final String RECIPE_BOOK_TAG = "recipeBook";
    private static final Logger LOGGER = LogManager.getLogger();

    public RecipeBookServer() {}

    public int a(Collection<IRecipe<?>> collection, EntityPlayer entityplayer) {
        List<MinecraftKey> list = Lists.newArrayList();
        int i = 0;
        Iterator iterator = collection.iterator();

        while (iterator.hasNext()) {
            IRecipe<?> irecipe = (IRecipe) iterator.next();
            MinecraftKey minecraftkey = irecipe.getKey();

            if (!this.known.contains(minecraftkey) && !irecipe.isComplex() && CraftEventFactory.handlePlayerRecipeListUpdateEvent(entityplayer, minecraftkey)) { // CraftBukkit
                this.a(minecraftkey);
                this.d(minecraftkey);
                list.add(minecraftkey);
                CriterionTriggers.RECIPE_UNLOCKED.a(entityplayer, irecipe);
                ++i;
            }
        }

        this.a(PacketPlayOutRecipes.Action.ADD, entityplayer, (List) list);
        return i;
    }

    public int b(Collection<IRecipe<?>> collection, EntityPlayer entityplayer) {
        List<MinecraftKey> list = Lists.newArrayList();
        int i = 0;
        Iterator iterator = collection.iterator();

        while (iterator.hasNext()) {
            IRecipe<?> irecipe = (IRecipe) iterator.next();
            MinecraftKey minecraftkey = irecipe.getKey();

            if (this.known.contains(minecraftkey)) {
                this.c(minecraftkey);
                list.add(minecraftkey);
                ++i;
            }
        }

        this.a(PacketPlayOutRecipes.Action.REMOVE, entityplayer, (List) list);
        return i;
    }

    private void a(PacketPlayOutRecipes.Action packetplayoutrecipes_action, EntityPlayer entityplayer, List<MinecraftKey> list) {
        if (entityplayer.connection == null) return; // SPIGOT-4478 during PlayerLoginEvent
        entityplayer.connection.sendPacket(new PacketPlayOutRecipes(packetplayoutrecipes_action, list, Collections.emptyList(), this.a()));
    }

    public NBTTagCompound save() {
        NBTTagCompound nbttagcompound = new NBTTagCompound();

        this.a().b(nbttagcompound);
        NBTTagList nbttaglist = new NBTTagList();
        Iterator iterator = this.known.iterator();

        while (iterator.hasNext()) {
            MinecraftKey minecraftkey = (MinecraftKey) iterator.next();

            nbttaglist.add(NBTTagString.a(minecraftkey.toString()));
        }

        nbttagcompound.set("recipes", nbttaglist);
        NBTTagList nbttaglist1 = new NBTTagList();
        Iterator iterator1 = this.highlight.iterator();

        while (iterator1.hasNext()) {
            MinecraftKey minecraftkey1 = (MinecraftKey) iterator1.next();

            nbttaglist1.add(NBTTagString.a(minecraftkey1.toString()));
        }

        nbttagcompound.set("toBeDisplayed", nbttaglist1);
        return nbttagcompound;
    }

    public void a(NBTTagCompound nbttagcompound, CraftingManager craftingmanager) {
        this.a(RecipeBookSettings.a(nbttagcompound));
        NBTTagList nbttaglist = nbttagcompound.getList("recipes", 8);

        this.a(nbttaglist, this::a, craftingmanager);
        NBTTagList nbttaglist1 = nbttagcompound.getList("toBeDisplayed", 8);

        this.a(nbttaglist1, this::f, craftingmanager);
    }

    private void a(NBTTagList nbttaglist, Consumer<IRecipe<?>> consumer, CraftingManager craftingmanager) {
        for (int i = 0; i < nbttaglist.size(); ++i) {
            String s = nbttaglist.getString(i);

            try {
                MinecraftKey minecraftkey = new MinecraftKey(s);
                Optional<? extends IRecipe<?>> optional = craftingmanager.getRecipe(minecraftkey);

                if (!optional.isPresent()) {
                    RecipeBookServer.LOGGER.error("Tried to load unrecognized recipe: {} removed now.", minecraftkey);
                } else {
                    consumer.accept((IRecipe) optional.get());
                }
            } catch (ResourceKeyInvalidException resourcekeyinvalidexception) {
                RecipeBookServer.LOGGER.error("Tried to load improperly formatted recipe: {} removed now.", s);
            }
        }

    }

    public void a(EntityPlayer entityplayer) {
        entityplayer.connection.sendPacket(new PacketPlayOutRecipes(PacketPlayOutRecipes.Action.INIT, this.known, this.highlight, this.a()));
    }
}
