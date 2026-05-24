package fr.gcjojo.worldscolliding.entity;

import fr.gcjojo.worldscolliding.ModSounds;

import fr.gcjojo.worldscolliding.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class ScourgeEntity extends PathfinderMob implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ScourgeEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 69.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public void tick() {
        if(level().isClientSide())
            return;

        level().getEntities(this, getBoundingBox().inflate(10), entity -> entity instanceof Player).forEach(player -> {
         if(!(player instanceof Player))
             return;

         boolean isInDialogue = player.getPersistentData().getBoolean("IsInDialogue");
         if(isInDialogue)
             return;

         String currentChapter = player.getPersistentData().getString("CurrentChapter");
         String lastReadChapter = player.getPersistentData().getString("LastReadChapter");
         if (currentChapter.isEmpty()) {
             currentChapter = "chapter_0_set";
             player.getPersistentData().putString("CurrentChapter", currentChapter);
         }

         if(lastReadChapter.isEmpty() || !lastReadChapter.equals(currentChapter))
         {
            player.getPersistentData().putBoolean("IsInDialogue", true);
            ModNetwork.sendToPlayer(new ModNetwork.OpenDialoguePacket(currentChapter), (ServerPlayer) player);
         }
        });
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(3.0f, 2.0f);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event ->
                event.setAndContinue(RawAnimation.begin().thenLoop("idle"))));

        controllers.add(new AnimationController<>(this, "seal_controller", 0, event -> PlayState.CONTINUE)
                .triggerableAnim("sceal1", RawAnimation.begin().thenPlay("sceal1"))
                .triggerableAnim("sceal2", RawAnimation.begin().thenPlay("sceal2"))
                .triggerableAnim("sceal3", RawAnimation.begin().thenPlay("sceal3"))
                .triggerableAnim("sceal4", RawAnimation.begin().thenPlay("sceal4"))
                .triggerableAnim("sceal5", RawAnimation.begin().thenPlay("sceal5"))
                .triggerableAnim("sceal6", RawAnimation.begin().thenPlay("sceal6"))
                .triggerableAnim("sceal7", RawAnimation.begin().thenPlay("sceal7"))
                .triggerableAnim("sceal8", RawAnimation.begin().thenPlay("sceal8"))
                .setSoundKeyframeHandler(event -> {
                    String sound = event.getKeyframeData().getSound();
                    if (sound.contains("sword_draw")) {
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.SWORD_DRAW.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
                    } else if (sound.contains("protoss")) {
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.PROTOSS_ELECTRIC.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
                    } else if (sound.contains("master_sword") || sound.contains("zeldamastersword")) {
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.MASTER_SWORD.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
                    }
                })
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        entity.push(this);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) {
            return super.hurt(source, amount);
        }
        return false;
    }
}