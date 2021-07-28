package net.minecraft.world.entity.animal;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.IInventory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.IShearable;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreed;
import net.minecraft.world.entity.ai.goal.PathfinderGoalEatTile;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFollowParent;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPanic;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.PathfinderGoalTempt;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.ItemDye;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.item.crafting.Recipes;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootTables;

// CraftBukkit start
import net.minecraft.world.inventory.InventoryCraftResult;
import net.minecraft.world.item.Item;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.SheepRegrowWoolEvent;
import org.bukkit.inventory.InventoryView;
// CraftBukkit end

public class EntitySheep extends EntityAnimal implements IShearable {

    private static final int EAT_ANIMATION_TICKS = 40;
    private static final DataWatcherObject<Byte> DATA_WOOL_ID = DataWatcher.a(EntitySheep.class, DataWatcherRegistry.BYTE);
    private static final Map<EnumColor, IMaterial> ITEM_BY_DYE = (Map) SystemUtils.a(Maps.newEnumMap(EnumColor.class), (enummap) -> { // CraftBukkit - decompile error
        enummap.put(EnumColor.WHITE, Blocks.WHITE_WOOL);
        enummap.put(EnumColor.ORANGE, Blocks.ORANGE_WOOL);
        enummap.put(EnumColor.MAGENTA, Blocks.MAGENTA_WOOL);
        enummap.put(EnumColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_WOOL);
        enummap.put(EnumColor.YELLOW, Blocks.YELLOW_WOOL);
        enummap.put(EnumColor.LIME, Blocks.LIME_WOOL);
        enummap.put(EnumColor.PINK, Blocks.PINK_WOOL);
        enummap.put(EnumColor.GRAY, Blocks.GRAY_WOOL);
        enummap.put(EnumColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_WOOL);
        enummap.put(EnumColor.CYAN, Blocks.CYAN_WOOL);
        enummap.put(EnumColor.PURPLE, Blocks.PURPLE_WOOL);
        enummap.put(EnumColor.BLUE, Blocks.BLUE_WOOL);
        enummap.put(EnumColor.BROWN, Blocks.BROWN_WOOL);
        enummap.put(EnumColor.GREEN, Blocks.GREEN_WOOL);
        enummap.put(EnumColor.RED, Blocks.RED_WOOL);
        enummap.put(EnumColor.BLACK, Blocks.BLACK_WOOL);
    });
    private static final Map<EnumColor, float[]> COLORARRAY_BY_COLOR = Maps.newEnumMap((Map) Arrays.stream(EnumColor.values()).collect(Collectors.toMap((enumcolor) -> {
        return enumcolor;
    }, EntitySheep::c)));
    private int eatAnimationTick;
    private PathfinderGoalEatTile eatBlockGoal;

    private static float[] c(EnumColor enumcolor) {
        if (enumcolor == EnumColor.WHITE) {
            return new float[]{0.9019608F, 0.9019608F, 0.9019608F};
        } else {
            float[] afloat = enumcolor.getColor();
            float f = 0.75F;

            return new float[]{afloat[0] * 0.75F, afloat[1] * 0.75F, afloat[2] * 0.75F};
        }
    }

    public static float[] a(EnumColor enumcolor) {
        return (float[]) EntitySheep.COLORARRAY_BY_COLOR.get(enumcolor);
    }

    public EntitySheep(EntityTypes<? extends EntitySheep> entitytypes, World world) {
        super(entitytypes, world);
    }

    @Override
    protected void initPathfinder() {
        this.eatBlockGoal = new PathfinderGoalEatTile(this);
        this.goalSelector.a(0, new PathfinderGoalFloat(this));
        this.goalSelector.a(1, new PathfinderGoalPanic(this, 1.25D));
        this.goalSelector.a(2, new PathfinderGoalBreed(this, 1.0D));
        this.goalSelector.a(3, new PathfinderGoalTempt(this, 1.1D, RecipeItemStack.a(Items.WHEAT), false));
        this.goalSelector.a(4, new PathfinderGoalFollowParent(this, 1.1D));
        this.goalSelector.a(5, this.eatBlockGoal);
        this.goalSelector.a(6, new PathfinderGoalRandomStrollLand(this, 1.0D));
        this.goalSelector.a(7, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 6.0F));
        this.goalSelector.a(8, new PathfinderGoalRandomLookaround(this));
    }

    @Override
    protected void mobTick() {
        this.eatAnimationTick = this.eatBlockGoal.g();
        super.mobTick();
    }

    @Override
    public void movementTick() {
        if (this.level.isClientSide) {
            this.eatAnimationTick = Math.max(0, this.eatAnimationTick - 1);
        }

        super.movementTick();
    }

    public static AttributeProvider.Builder p() {
        return EntityInsentient.w().a(GenericAttributes.MAX_HEALTH, 8.0D).a(GenericAttributes.MOVEMENT_SPEED, 0.23000000417232513D);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(EntitySheep.DATA_WOOL_ID, (byte) 0);
    }

    @Override
    public MinecraftKey getDefaultLootTable() {
        if (this.isSheared()) {
            return this.getEntityType().j();
        } else {
            switch (this.getColor()) {
                case WHITE:
                default:
                    return LootTables.SHEEP_WHITE;
                case ORANGE:
                    return LootTables.SHEEP_ORANGE;
                case MAGENTA:
                    return LootTables.SHEEP_MAGENTA;
                case LIGHT_BLUE:
                    return LootTables.SHEEP_LIGHT_BLUE;
                case YELLOW:
                    return LootTables.SHEEP_YELLOW;
                case LIME:
                    return LootTables.SHEEP_LIME;
                case PINK:
                    return LootTables.SHEEP_PINK;
                case GRAY:
                    return LootTables.SHEEP_GRAY;
                case LIGHT_GRAY:
                    return LootTables.SHEEP_LIGHT_GRAY;
                case CYAN:
                    return LootTables.SHEEP_CYAN;
                case PURPLE:
                    return LootTables.SHEEP_PURPLE;
                case BLUE:
                    return LootTables.SHEEP_BLUE;
                case BROWN:
                    return LootTables.SHEEP_BROWN;
                case GREEN:
                    return LootTables.SHEEP_GREEN;
                case RED:
                    return LootTables.SHEEP_RED;
                case BLACK:
                    return LootTables.SHEEP_BLACK;
            }
        }
    }

    @Override
    public void a(byte b0) {
        if (b0 == 10) {
            this.eatAnimationTick = 40;
        } else {
            super.a(b0);
        }

    }

    public float z(float f) {
        return this.eatAnimationTick <= 0 ? 0.0F : (this.eatAnimationTick >= 4 && this.eatAnimationTick <= 36 ? 1.0F : (this.eatAnimationTick < 4 ? ((float) this.eatAnimationTick - f) / 4.0F : -((float) (this.eatAnimationTick - 40) - f) / 4.0F));
    }

    public float A(float f) {
        if (this.eatAnimationTick > 4 && this.eatAnimationTick <= 36) {
            float f1 = ((float) (this.eatAnimationTick - 4) - f) / 32.0F;

            return 0.62831855F + 0.21991149F * MathHelper.sin(f1 * 28.7F);
        } else {
            return this.eatAnimationTick > 0 ? 0.62831855F : this.getXRot() * 0.017453292F;
        }
    }

    @Override
    public EnumInteractionResult b(EntityHuman entityhuman, EnumHand enumhand) {
        ItemStack itemstack = entityhuman.b(enumhand);

        if (itemstack.a(Items.SHEARS)) {
            if (!this.level.isClientSide && this.canShear()) {
                // CraftBukkit start
                if (!CraftEventFactory.handlePlayerShearEntityEvent(entityhuman, this, itemstack, enumhand)) {
                    return EnumInteractionResult.PASS;
                }
                // CraftBukkit end
                this.shear(SoundCategory.PLAYERS);
                this.a(GameEvent.SHEAR, (Entity) entityhuman);
                itemstack.damage(1, entityhuman, (entityhuman1) -> {
                    entityhuman1.broadcastItemBreak(enumhand);
                });
                return EnumInteractionResult.SUCCESS;
            } else {
                return EnumInteractionResult.CONSUME;
            }
        } else {
            return super.b(entityhuman, enumhand);
        }
    }

    @Override
    public void shear(SoundCategory soundcategory) {
        this.level.playSound((EntityHuman) null, (Entity) this, SoundEffects.SHEEP_SHEAR, soundcategory, 1.0F, 1.0F);
        this.setSheared(true);
        int i = 1 + this.random.nextInt(3);

        for (int j = 0; j < i; ++j) {
            this.forceDrops = true; // CraftBukkit
            EntityItem entityitem = this.a((IMaterial) EntitySheep.ITEM_BY_DYE.get(this.getColor()), 1);
            this.forceDrops = false; // CraftBukkit

            if (entityitem != null) {
                entityitem.setMot(entityitem.getMot().add((double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F), (double) (this.random.nextFloat() * 0.05F), (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F)));
            }
        }

    }

    @Override
    public boolean canShear() {
        return this.isAlive() && !this.isSheared() && !this.isBaby();
    }

    @Override
    public void saveData(NBTTagCompound nbttagcompound) {
        super.saveData(nbttagcompound);
        nbttagcompound.setBoolean("Sheared", this.isSheared());
        nbttagcompound.setByte("Color", (byte) this.getColor().getColorIndex());
    }

    @Override
    public void loadData(NBTTagCompound nbttagcompound) {
        super.loadData(nbttagcompound);
        this.setSheared(nbttagcompound.getBoolean("Sheared"));
        this.setColor(EnumColor.fromColorIndex(nbttagcompound.getByte("Color")));
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.SHEEP_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource damagesource) {
        return SoundEffects.SHEEP_HURT;
    }

    @Override
    protected SoundEffect getSoundDeath() {
        return SoundEffects.SHEEP_DEATH;
    }

    @Override
    protected void b(BlockPosition blockposition, IBlockData iblockdata) {
        this.playSound(SoundEffects.SHEEP_STEP, 0.15F, 1.0F);
    }

    public EnumColor getColor() {
        return EnumColor.fromColorIndex((Byte) this.entityData.get(EntitySheep.DATA_WOOL_ID) & 15);
    }

    public void setColor(EnumColor enumcolor) {
        byte b0 = (Byte) this.entityData.get(EntitySheep.DATA_WOOL_ID);

        this.entityData.set(EntitySheep.DATA_WOOL_ID, (byte) (b0 & 240 | enumcolor.getColorIndex() & 15));
    }

    public boolean isSheared() {
        return ((Byte) this.entityData.get(EntitySheep.DATA_WOOL_ID) & 16) != 0;
    }

    public void setSheared(boolean flag) {
        byte b0 = (Byte) this.entityData.get(EntitySheep.DATA_WOOL_ID);

        if (flag) {
            this.entityData.set(EntitySheep.DATA_WOOL_ID, (byte) (b0 | 16));
        } else {
            this.entityData.set(EntitySheep.DATA_WOOL_ID, (byte) (b0 & -17));
        }

    }

    public static EnumColor a(Random random) {
        int i = random.nextInt(100);

        return i < 5 ? EnumColor.BLACK : (i < 10 ? EnumColor.GRAY : (i < 15 ? EnumColor.LIGHT_GRAY : (i < 18 ? EnumColor.BROWN : (random.nextInt(500) == 0 ? EnumColor.PINK : EnumColor.WHITE))));
    }

    @Override
    public EntitySheep createChild(WorldServer worldserver, EntityAgeable entityageable) {
        EntitySheep entitysheep = (EntitySheep) entityageable;
        EntitySheep entitysheep1 = (EntitySheep) EntityTypes.SHEEP.a((World) worldserver);

        entitysheep1.setColor(this.a((EntityAnimal) this, (EntityAnimal) entitysheep));
        return entitysheep1;
    }

    @Override
    public void blockEaten() {
        // CraftBukkit start
        SheepRegrowWoolEvent event = new SheepRegrowWoolEvent((org.bukkit.entity.Sheep) this.getBukkitEntity());
        this.level.getCraftServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) return;
        // CraftBukkit end
        this.setSheared(false);
        if (this.isBaby()) {
            this.setAge(60);
        }

    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess worldaccess, DifficultyDamageScaler difficultydamagescaler, EnumMobSpawn enummobspawn, @Nullable GroupDataEntity groupdataentity, @Nullable NBTTagCompound nbttagcompound) {
        this.setColor(a(worldaccess.getRandom()));
        return super.prepare(worldaccess, difficultydamagescaler, enummobspawn, groupdataentity, nbttagcompound);
    }

    private EnumColor a(EntityAnimal entityanimal, EntityAnimal entityanimal1) {
        EnumColor enumcolor = ((EntitySheep) entityanimal).getColor();
        EnumColor enumcolor1 = ((EntitySheep) entityanimal1).getColor();
        InventoryCrafting inventorycrafting = a(enumcolor, enumcolor1);
        Optional<Item> optional = this.level.getCraftingManager().craft(Recipes.CRAFTING, inventorycrafting, this.level).map((recipecrafting) -> { // Eclipse fail
            return recipecrafting.a(inventorycrafting); // CraftBukkit - decompile error
        }).map(ItemStack::getItem);

        Objects.requireNonNull(ItemDye.class);
        optional = optional.filter(ItemDye.class::isInstance);
        Objects.requireNonNull(ItemDye.class);
        return (EnumColor) optional.map(ItemDye.class::cast).map(ItemDye::d).orElseGet(() -> {
            return this.level.random.nextBoolean() ? enumcolor : enumcolor1;
        });
    }

    private static InventoryCrafting a(EnumColor enumcolor, EnumColor enumcolor1) {
        InventoryCrafting inventorycrafting = new InventoryCrafting(new Container((Containers) null, -1) {
            @Override
            public boolean canUse(EntityHuman entityhuman) {
                return false;
            }

            // CraftBukkit start
            @Override
            public InventoryView getBukkitView() {
                return null; // TODO: O.O
            }
            // CraftBukkit end
        }, 2, 1);

        inventorycrafting.setItem(0, new ItemStack(ItemDye.a(enumcolor)));
        inventorycrafting.setItem(1, new ItemStack(ItemDye.a(enumcolor1)));
        inventorycrafting.resultInventory = new InventoryCraftResult(); // CraftBukkit - add result slot for event
        return inventorycrafting;
    }

    @Override
    protected float b(EntityPose entitypose, EntitySize entitysize) {
        return 0.95F * entitysize.height;
    }
}
