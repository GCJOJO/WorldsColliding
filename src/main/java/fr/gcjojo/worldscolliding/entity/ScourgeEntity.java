package fr.gcjojo.worldscolliding.entity;

import fr.gcjojo.worldscolliding.ModSounds;
import fr.gcjojo.worldscolliding.network.ModNetwork;
import io.github.gcjojo.blablalib.BlablaLib;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.core.jmx.Server;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ScourgeEntity extends PathfinderMob implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static final EntityDataAccessor<Boolean> ASCENDING = SynchedEntityData.defineId(ScourgeEntity.class, EntityDataSerializers.BOOLEAN);
    public int ascendTimer = 0;

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
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(3.0f, 2.0f);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ASCENDING, false);
    }

    @Override
    public void tick() {
        super.tick();

        if(level().isClientSide())
            return;

        level().getEntities(this, getBoundingBox().inflate(5), entity -> entity instanceof Player).forEach(playerEntity -> {
            if(!(playerEntity instanceof ServerPlayer))
                return;

            ServerPlayer player = (ServerPlayer) playerEntity;
            if(!this.getPersistentData().contains("Player"))
                this.getPersistentData().putUUID("Player", player.getUUID());

            if(BlablaLib.isPlayerInDialogue(player))
                return;

            String currentChapter = BlablaLib.getPlayerDialogue(player);
            String lastReadChapter = BlablaLib.getPlayerLastReadDialogue(player);

            if (currentChapter.isEmpty())
                currentChapter = "worldscolliding:chapter_0_set";

            if(lastReadChapter.isEmpty() || !lastReadChapter.equals(currentChapter))
                BlablaLib.openDialogue(player, currentChapter);
        });

        if (this.entityData.get(ASCENDING)) {
            this.ascendTimer++;
            this.setDeltaMovement(0, 0.05, 0);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + this.random.nextDouble() * 2, this.getZ(), 10, 0.3, 0.5, 0.3, 0.0);

                double orbitRadius = 2.5;
                double time = this.ascendTimer * 0.3;
                double orbitX = this.getX() + Math.cos(time) * orbitRadius;
                double orbitZ = this.getZ() + Math.sin(time) * orbitRadius;

                serverLevel.sendParticles(ParticleTypes.SQUID_INK, orbitX, this.getY() + this.random.nextDouble() * 2, orbitZ, 5, 0.2, 0.5, 0.2, 0.0);

                if (this.ascendTimer == 200) {
                    this.playSound(ModSounds.LAUGH.get(), 1.0f, 1.0f);
                }

                if (this.ascendTimer >= 300) {
                    this.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 2, 0, 0, 0, 0);
                    CompoundTag mobNbt = this.getPersistentData();
                    if(mobNbt.contains("Player")){
                        Player player = serverLevel.getPlayerByUUID(mobNbt.getUUID("Player"));
                        if(player != null){
                            ServerPlayer serverPlayer = (ServerPlayer) player;
                            MutableComponent msg = Component.translatable("worldscolliding.dialogue.scourge_ban_message").withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC);
                            serverPlayer.sendSystemMessage(msg, false);

                            BlablaLib.openDialogue(serverPlayer, "worldscolliding:chapter_7_after_scourge_banned_set");
                            serverPlayer.getPersistentData().putBoolean("RespawnsScourge", true);
                        }

                    }

                    this.discard();
                }
            }
        }
    }

    @Override
    public boolean isPersistenceRequired(){ return true; }

    @Override
    public boolean requiresCustomPersistence() { return true; }

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
                    if (this.level().isClientSide) {
                        if (sound.contains("sword_draw")) {
                            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ModSounds.SWORD_DRAW.get(), SoundSource.HOSTILE, 1.0f, 1.0f, false);
                        } else if (sound.contains("protoss_electric")) {
                            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ModSounds.PROTOSS_ELECTRIC.get(), SoundSource.HOSTILE, 1.0f, 1.0f, false);
                        } else if (sound.contains("zelda_master_sword") ) {
                            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ModSounds.MASTER_SWORD.get(), SoundSource.HOSTILE, 1.0f, 1.0f, false);
                        }
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