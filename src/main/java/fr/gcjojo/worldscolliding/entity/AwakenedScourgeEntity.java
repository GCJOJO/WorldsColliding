package fr.gcjojo.worldscolliding.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import fr.gcjojo.worldscolliding.ModSounds;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AwakenedScourgeEntity extends Monster implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(AwakenedScourgeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HAS_SWORD = SynchedEntityData.defineId(AwakenedScourgeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SPAWNED = SynchedEntityData.defineId(AwakenedScourgeEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_DYING = SynchedEntityData.defineId(AwakenedScourgeEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_PLAYING_MUSIC = SynchedEntityData.defineId(AwakenedScourgeEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = (ServerBossEvent)(new ServerBossEvent(Component.literal("The Awakened Scourge"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS)).setDarkenScreen(true);

    public int attackTick = 0;
    private int spellChoice = 0;
    private int deathTimer = 0;

    private boolean hasSpawnedPhantomsPhase1 = false;
    private boolean hasSpawnedPhantomsPhase2 = false;

    private final Map<UUID, GameType> previousGameModes = new HashMap<>();

    public AwakenedScourgeEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.7D)
                .add(Attributes.ATTACK_DAMAGE, 25.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FLYING_SPEED, 0.7D)
                .add(Attributes.FOLLOW_RANGE, 100.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    public boolean hasLineOfSight(Entity entity) {
        return true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, 1);
        this.entityData.define(HAS_SWORD, false);
        this.entityData.define(SPAWNED, false);
        this.entityData.define(IS_DYING, false);
        this.entityData.define(IS_PLAYING_MUSIC, false);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            if (this.entityData.get(IS_DYING)) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("death"));
            }

            int state = this.entityData.get(STATE);
            boolean hasSword = this.entityData.get(HAS_SWORD);

            switch (state) {
                case 1: return event.setAndContinue(RawAnimation.begin().thenPlay("spawn"));
                case 2: return event.setAndContinue(RawAnimation.begin().thenPlay("attack_ball"));
                case 3: return event.setAndContinue(RawAnimation.begin().thenPlay("attack_tour"));
                case 4: return event.setAndContinue(RawAnimation.begin().thenPlay("attack_beam"));
                case 5: return event.setAndContinue(RawAnimation.begin().thenPlay("summon_sword"));
                case 6: return event.setAndContinue(RawAnimation.begin().thenPlay("attack_swing"));
                case 7: return event.setAndContinue(RawAnimation.begin().thenPlay("attack_ground"));
                case 8: return event.setAndContinue(RawAnimation.begin().thenPlay("attack_spell"));
                case 9: return event.setAndContinue(RawAnimation.begin().thenPlay("attack_back"));
                case 10: return event.setAndContinue(RawAnimation.begin().thenPlay("attack_back_sword"));
                case 11: return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("shield"));
                default:
                    if (event.isMoving()) {
                        return event.setAndContinue(RawAnimation.begin().thenLoop(hasSword ? "iddle_sword" : "iddle"));
                    }
                    return event.setAndContinue(RawAnimation.begin().thenLoop(hasSword ? "iddle_sword" : "iddle"));
            }
        }).setSoundKeyframeHandler(event -> {
            String sound = event.getKeyframeData().getSound();
            if (this.level().isClientSide) {
                if (sound.contains("sword_draw")) {
                    this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ModSounds.SWORD_DRAW.get(), SoundSource.HOSTILE, 1.0f, 1.0f, false);
                } else if (sound.contains("victory")) {
                    this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ModSounds.VICTORY.get(), SoundSource.HOSTILE, 1.0f, 1.0f, false);
                } else if (sound.contains("protoss")) {
                    this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ModSounds.PROTOSS_ELECTRIC.get(), SoundSource.HOSTILE, 1.0f, 1.0f, false);
                } else if (sound.contains("master_sword") || sound.contains("zeldamastersword") || sound.contains("zelda_master_sword")) {
                    this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ModSounds.MASTER_SWORD.get(), SoundSource.HOSTILE, 1.0f, 1.0f, false);
                } else if (sound.contains("enterganondorf")) {
                    this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ModSounds.ENTER_GANONDORF.get(), SoundSource.HOSTILE, 1.0f, 1.0f, false);
                } else if (sound.contains("laugh")) {
                    this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ModSounds.LAUGH.get(), SoundSource.HOSTILE, 1.0f, 1.0f, false);
                } else if (sound.contains("switchclick")) {
                    this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ModSounds.SWITCH_CLICK.get(), SoundSource.HOSTILE, 1.0f, 1.0f, false);
                }
            }
        }));
    }

    private void spawnRedLine(ServerLevel level, Vec3 start, Vec3 dir) {
        Vec3 end = start.add(dir.scale(150.0));
        ClipContext context = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this);
        BlockHitResult result = level.clip(context);
        Vec3 hitPos = result.getLocation();

        double distance = start.distanceTo(hitPos);
        for (double i = 0; i < distance; i += 0.5) {
            Vec3 pos = start.add(dir.scale(i));
            level.sendParticles(DustParticleOptions.REDSTONE, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!this.entityData.get(IS_DYING)) {
            this.entityData.set(IS_DYING, true);
            this.setHealth(1.0f);
            this.setInvulnerable(true);
            this.playSound(ModSounds.VICTORY.get(), 1.0f, 1.0f);
            if(source.getEntity() instanceof Player) {
                Player player = (Player) source.getEntity();
                player.getPersistentData().putBoolean("ShowCredits", true);
            }
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new AwakenedScourgeAttackGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            List<ServerPlayer> players = this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(64.0));
            for (ServerPlayer player : players) {
                if (player.isDeadOrDying() && this.getHealth() < this.getMaxHealth()) {
                    this.setHealth(this.getMaxHealth());
                    this.entityData.set(HAS_SWORD, false);
                    this.hasSpawnedPhantomsPhase1 = false;
                    this.hasSpawnedPhantomsPhase2 = false;
                    this.setTarget(null);
                    break;
                }
            }

            if (this.entityData.get(STATE) == 11) {
                this.heal(10.0F);
            }
        }

        if (this.entityData.get(IS_DYING)) {
            this.deathTimer++;
            if (this.level() instanceof ServerLevel serverLevel) {
                if (this.deathTimer == 1) {
                    List<ServerPlayer> players = this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(64.0));
                    Vec3 lookDir = this.getLookAngle();
                    Vec3 camPos = this.position().add(lookDir.scale(5.0)).add(0, 1.0, 0);
                    float yRot = this.getYRot() + 180.0F;

                    for (ServerPlayer player : players) {
                        previousGameModes.put(player.getUUID(), player.gameMode.getGameModeForPlayer());
                        player.setGameMode(GameType.SPECTATOR);
                        player.teleportTo(serverLevel, camPos.x, camPos.y, camPos.z, yRot, 0.0F);
                    }
                }

                if (this.deathTimer >= 180 && this.deathTimer < 400) {
                    float progress = (this.deathTimer - 180) / 220.0f;
                    if (this.random.nextFloat() > progress) {
                        double x = this.getX();
                        double z = this.getZ();
                        for (int y = 0; y < 15; y++) {
                            serverLevel.sendParticles(ParticleTypes.END_ROD, x, this.getY() + y, z, 2, 0.5, 0.5, 0.5, 0.0);
                        }

                        double orbitRadius = 6.0;
                        double time = this.deathTimer * 0.1;
                        double orbitX = x + Math.cos(time) * orbitRadius;
                        double orbitZ = z + Math.sin(time) * orbitRadius;
                        for (int y = 0; y < 15; y++) {
                            serverLevel.sendParticles(ParticleTypes.END_ROD, orbitX, this.getY() + y, orbitZ, 2, 0.5, 0.5, 0.5, 0.0);
                        }
                    }
                }

                if (this.deathTimer >= 260 && this.deathTimer < 280) {
                    Vec3 eyePos = this.getEyePosition();
                    Vec3 lookDir = this.getLookAngle();
                    Vec3 rotatedRight = new Vec3(-lookDir.z, 0, lookDir.x).normalize();
                    Vec3 rotatedLeft = new Vec3(lookDir.z, 0, -lookDir.x).normalize();

                    spawnRedLine(serverLevel, eyePos.add(0, -1.4, 0), rotatedRight);
                    spawnRedLine(serverLevel, eyePos.add(0, -1.4, 0), rotatedLeft);
                }

                if (this.deathTimer >= 300 && this.deathTimer <= 320) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0, 0, 0, 0);
                }

                if (this.deathTimer == 399) {
                    List<ServerPlayer> players = this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(64.0));
                    for (ServerPlayer player : players) {
                        GameType prev = previousGameModes.getOrDefault(player.getUUID(), GameType.SURVIVAL);
                        player.setGameMode(prev);
                    }
                }
            }
            if (this.deathTimer >= 400) {
                this.discard();
            }
            return;
        }

        if (this.level().isClientSide && this.entityData.get(SPAWNED)) {
            double px = this.getX() + (this.random.nextDouble() - 0.5) * 1.5;
            double py = this.getY() + 0.1;
            double pz = this.getZ() + (this.random.nextDouble() - 0.5) * 1.5;
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, px, py, pz, 0, 0.01, 0);
        }

        if (!this.level().isClientSide) {
            int currentState = this.entityData.get(STATE);

            if (currentState == 0 && this.entityData.get(SPAWNED)) {
                float hpPct = this.getHealth() / this.getMaxHealth();
                if (!hasSpawnedPhantomsPhase1 && hpPct <= 0.75f && !this.entityData.get(HAS_SWORD)) {
                    hasSpawnedPhantomsPhase1 = true;
                    triggerSpell(1);
                    return;
                }
                if (!hasSpawnedPhantomsPhase2 && hpPct <= 0.25f && this.entityData.get(HAS_SWORD)) {
                    hasSpawnedPhantomsPhase2 = true;
                    triggerSpell(1);
                    return;
                }
            }

            if (this.getTarget() != null) {
                double targetY = this.getTarget().getY() + 1.5;
                double dy = targetY - this.getY();
                this.setDeltaMovement(this.getDeltaMovement().x, dy * 0.1, this.getDeltaMovement().z);

                double dx = this.getTarget().getX() - this.getX();
                double dz = this.getTarget().getZ() - this.getZ();
                float rot = (float)(Mth.atan2(dz, dx) * (180F / Math.PI)) - 90.0F;
                this.setYRot(rot);
                this.yHeadRot = rot;
                this.yBodyRot = rot;
            } else {
                BlockPos posDown = this.blockPosition().below(2);
                if (!this.level().getBlockState(posDown).isAir()) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0, 0.02, 0));
                } else {
                    this.setDeltaMovement(this.getDeltaMovement().add(0, -0.02, 0));
                }
            }

            if (currentState == 1) {
                this.attackTick++;
                if (this.attackTick == 400) {
                    AABB aabb = new AABB(this.blockPosition()).inflate(50);
                    List<ServerPlayer> players = this.level().getEntitiesOfClass(ServerPlayer.class, aabb);
                    for (ServerPlayer p : players) {
                        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
                        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4The Scourge")));
                        this.entityData.set(IS_PLAYING_MUSIC, true);
                    }
                }
                if (this.attackTick >= 500) {
                    this.entityData.set(SPAWNED, true);
                    this.setAttackState(0);
                }
                return;
            }

            if (!this.entityData.get(HAS_SWORD) && this.getHealth() <= this.getMaxHealth() * 0.5f && currentState != 5 && this.entityData.get(SPAWNED)) {
                this.setAttackState(5);
            }

            if (currentState == 5) {
                this.attackTick++;
                if (this.attackTick >= 200) {
                    this.entityData.set(HAS_SWORD, true);
                    this.setAttackState(0);
                }
            }

            if (currentState == 2) {
                this.attackTick++;
                if (this.attackTick == 15) {
                    if (this.getTarget() != null) {
                        Vec3 look = this.getLookAngle();
                        DragonFireball fireball = new DragonFireball(this.level(), this, look.x, look.y, look.z);
                        fireball.setPos(this.getX(), this.getY() + 1.5, this.getZ());
                        this.level().addFreshEntity(fireball);
                    }
                }
                if (this.attackTick >= 30) {
                    this.setAttackState(0);
                }
            }

            if (currentState == 3) {
                this.attackTick++;
                if (this.attackTick >= 40 && this.attackTick <= 80) {
                    AABB hitBox = new AABB(this.getX() - 3, this.getY() - 3, this.getZ() - 3, this.getX() + 3, this.getY() + 3, this.getZ() + 3);
                    List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != this);
                    for (LivingEntity target : targets) {
                        target.hurt(this.damageSources().mobAttack(this), 25.0f);
                    }
                }
                if (this.attackTick >= 80) {
                    this.setAttackState(0);
                }
            }

            if (currentState == 4) {
                this.attackTick++;
                if (this.attackTick >= 100 && this.attackTick <= 160) {
                    Vec3 look = this.getLookAngle();
                    look = new Vec3(look.x, 0, look.z).normalize();
                    Vec3 start = this.position().add(0, 0.5, 0);
                    for (int i = 1; i < 30; i++) {
                        Vec3 pos = start.add(look.scale(i));
                        if (!this.level().getBlockState(new BlockPos((int)pos.x, (int)pos.y, (int)pos.z)).isAir()) {
                            break;
                        }
                        ((ServerLevel)this.level()).sendParticles(DustParticleOptions.REDSTONE, pos.x, pos.y, pos.z, 5, 0.2, 0.2, 0.2, 0.0);
                        AABB hitBox = new AABB(pos.x - 1, pos.y - 1, pos.z - 1, pos.x + 1, pos.y + 1, pos.z + 1);
                        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != this);
                        for (LivingEntity target : targets) {
                            target.hurt(this.damageSources().magic(), 15.0f);
                        }
                    }
                }
                if (this.attackTick >= 180) {
                    this.setAttackState(0);
                }
            }

            if (currentState == 6) {
                this.attackTick++;
                if (this.attackTick >= 20 && this.attackTick <= 30) {
                    Vec3 look = this.getLookAngle();
                    Vec3 pos = this.position().add(look.scale(2));
                    AABB hitBox = new AABB(pos.x - 2, pos.y - 2, pos.z - 2, pos.x + 2, pos.y + 2, pos.z + 2);
                    List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != this);
                    for (LivingEntity target : targets) {
                        target.hurt(this.damageSources().mobAttack(this), 35.0f);
                    }
                }
                if (this.attackTick >= 40) {
                    this.setAttackState(0);
                }
            }

            if (currentState == 7) {
                this.attackTick++;
                if (this.attackTick >= 140 && this.attackTick <= 190) {
                    AABB hitBox = new AABB(this.getX() - 20, this.getY() - 10, this.getZ() - 20, this.getX() + 20, this.getY() + 10, this.getZ() + 20);
                    List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != this);
                    for (LivingEntity target : targets) {
                        target.hurt(this.damageSources().magic(), 20.0f);
                        ((ServerLevel)this.level()).sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY() + 1, target.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
                    }
                }
                if (this.attackTick >= 220) {
                    this.setAttackState(0);
                }
            }

            if (currentState == 8) {
                this.attackTick++;
                if (this.attackTick == 20) {
                    if (this.spellChoice == 0) {
                        if (this.getTarget() != null) {
                            Vec3 targetPos = this.getTarget().position();
                            Vec3 dir = this.position().subtract(targetPos).normalize();
                            Vec3 pushVector = dir.scale(-3.5).add(0, 1.5, 0);
                            this.getTarget().setDeltaMovement(pushVector);
                            this.getTarget().hurtMarked = true;
                            this.getTarget().hasImpulse = true;
                        }
                    } else if (this.spellChoice == 1) {
                        for (int i = 0; i < 5; i++) {
                            Phantom phantom = EntityType.PHANTOM.create(this.level());
                            if (phantom != null) {
                                phantom.moveTo(this.getX() + (this.random.nextDouble() - 0.5) * 4, this.getY() + 2, this.getZ() + (this.random.nextDouble() - 0.5) * 4);
                                if (this.getTarget() != null) {
                                    phantom.setTarget(this.getTarget());
                                }
                                this.level().addFreshEntity(phantom);
                            }
                        }
                    } else if (this.spellChoice == 2) {
                        if (this.getTarget() != null) {
                            ((ServerLevel)this.level()).sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1, this.getZ(), 100, 1.0, 2.0, 1.0, 0.05);
                            ((ServerLevel)this.level()).sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1, this.getZ(), 50, 1.0, 2.0, 1.0, 0.05);

                            Vec3 look = this.getTarget().getLookAngle();
                            Vec3 tpPos = this.getTarget().position().subtract(look.scale(3));
                            this.teleportTo(tpPos.x, tpPos.y, tpPos.z);

                            ((ServerLevel)this.level()).sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1, this.getZ(), 100, 1.0, 2.0, 1.0, 0.05);
                            ((ServerLevel)this.level()).sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1, this.getZ(), 50, 1.0, 2.0, 1.0, 0.05);
                            this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f);
                        }
                    } else if (this.spellChoice == 3) {
                        if (this.getTarget() != null) {
                            Vec3 targetPos = this.getTarget().position();
                            Vec3 dir = this.position().subtract(targetPos).normalize();
                            Vec3 pullVector = dir.scale(4.0).add(0, 0.5, 0);
                            this.getTarget().setDeltaMovement(pullVector);
                            this.getTarget().hurtMarked = true;
                            this.getTarget().hasImpulse = true;
                        }
                    }
                }
                if (this.attackTick >= 40) {
                    if (this.spellChoice == 2) {
                        this.setAttackState(this.entityData.get(HAS_SWORD) ? 6 : 3);
                    } else if (this.spellChoice == 1) {
                        this.setAttackState(11);
                    } else {
                        this.setAttackState(0);
                    }
                }
            }

            if (currentState == 9 || currentState == 10) {
                this.attackTick++;
                if (this.attackTick >= 15 && this.attackTick <= 25) {
                    Vec3 look = this.getLookAngle().scale(-1);
                    Vec3 pos = this.position().add(look.scale(2));
                    AABB hitBox = new AABB(pos.x - 3, pos.y - 2, pos.z - 3, pos.x + 3, pos.y + 2, pos.z + 3);
                    List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != this);
                    for (LivingEntity target : targets) {
                        target.hurt(this.damageSources().mobAttack(this), 30.0f);
                    }
                }
                if (this.attackTick >= 40) {
                    this.setAttackState(0);
                }
            }

            if (currentState == 11) {
                this.attackTick++;
                if (this.attackTick % 3 == 0) {
                    for (int k = 0; k < 3; k++) {
                        Vec3 dir = new Vec3(this.random.nextDouble() - 0.5, this.random.nextDouble() - 0.5, this.random.nextDouble() - 0.5).normalize();
                        Vec3 start = this.position().add(0, 0.2, 0);
                        for (int i = 1; i < 20; i++) {
                            Vec3 pos = start.add(dir.scale(i));
                            if (!this.level().getBlockState(new BlockPos((int)pos.x, (int)pos.y, (int)pos.z)).isAir()) {
                                break;
                            }
                            ((ServerLevel)this.level()).sendParticles(DustParticleOptions.REDSTONE, pos.x, pos.y, pos.z, 2, 0.1, 0.1, 0.1, 0.0);
                            AABB hitBox = new AABB(pos.x - 1, pos.y - 1, pos.z - 1, pos.x + 1, pos.y + 1, pos.z + 1);
                            List<Player> targets = this.level().getEntitiesOfClass(Player.class, hitBox);
                            for (Player target : targets) {
                                target.hurt(this.damageSources().magic(), 10.0f);
                            }
                        }
                    }
                }
                if (this.attackTick > 40) {
                    List<Phantom> allies = this.level().getEntitiesOfClass(Phantom.class, this.getBoundingBox().inflate(30));
                    if (allies.isEmpty()) {
                        this.setAttackState(0);
                    }
                }
            }
        }
    }

    public void setAttackState(int stateId) {
        this.entityData.set(STATE, stateId);
        this.attackTick = 0;
    }

    public void triggerSpell(int choice) {
        this.spellChoice = choice;
        this.setAttackState(8);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        int state = this.entityData.get(STATE);

        if (source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC) || source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)) {
            return false;
        }

        if (state == 1 || state == 5 || state == 11) {
            return false;
        }
        if (!this.entityData.get(SPAWNED)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    class AwakenedScourgeAttackGoal extends Goal {
        private final AwakenedScourgeEntity mob;
        private int attackCooldown;
        private int lastAttackId = 0;
        private int consecutiveAttackCount = 0;
        private int distantAttackCount = 0;

        public AwakenedScourgeAttackGoal(AwakenedScourgeEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.mob.getTarget() != null && this.mob.entityData.get(STATE) == 0 && this.mob.entityData.get(SPAWNED);
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null) return;

            double distance = this.mob.distanceToSqr(target);
            boolean hasSword = this.mob.entityData.get(HAS_SWORD);

            Vec3 toTarget = target.position().subtract(this.mob.position()).normalize();
            boolean isBehind = this.mob.getLookAngle().dot(toTarget) < -0.2;

            if (distance > 64.0) {
                this.mob.getNavigation().moveTo(target, 1.5);
            } else if (distance < 9.0 && !hasSword) {
                Vec3 fleeDir = this.mob.position().subtract(target.position()).normalize().scale(5);
                this.mob.getNavigation().moveTo(this.mob.getX() + fleeDir.x, this.mob.getY(), this.mob.getZ() + fleeDir.z, 1.5);
            } else {
                this.mob.getNavigation().moveTo(target, 1.5);
            }

            if (this.attackCooldown > 0) {
                this.attackCooldown--;
                return;
            }

            int attackId = 0;

            if (isBehind) {
                attackId = hasSword ? 10 : 9;
                this.attackCooldown = 40;
            } else {
                if (hasSword) {
                    if (distance < 25.0) {
                        if (this.lastAttackId == 6 && this.consecutiveAttackCount >= 2) {
                            attackId = this.mob.random.nextBoolean() ? 8 : 7;
                            if (attackId == 8) {
                                this.mob.spellChoice = 0;
                            }
                            this.attackCooldown = 20;
                        } else {
                            attackId = 6;
                            this.attackCooldown = 15;
                        }
                    } else if (distance >= 25.0 && distance < 484.0) {
                        attackId = 8;
                        this.mob.spellChoice = this.mob.random.nextBoolean() ? 0 : 3;
                        this.attackCooldown = 40;
                    } else {
                        attackId = 8;
                        this.mob.spellChoice = 2;
                        this.attackCooldown = 20;
                    }
                } else {
                    if (distance < 36.0) {
                        if (this.lastAttackId == 3 && this.consecutiveAttackCount >= 2) {
                            attackId = 8;
                            this.mob.spellChoice = 0;
                            this.attackCooldown = 20;
                        } else {
                            attackId = 3;
                            this.attackCooldown = 30;
                        }
                    } else {
                        if (this.distantAttackCount >= 2) {
                            attackId = 8;
                            this.mob.spellChoice = this.mob.random.nextBoolean() ? 0 : 3;
                            this.attackCooldown = 40;
                        } else {
                            attackId = this.mob.random.nextBoolean() ? 2 : 4;
                            this.attackCooldown = (attackId == 2) ? 20 : 60;
                        }
                    }
                }
            }

            if (this.lastAttackId == 8 && this.mob.spellChoice == 0 && !hasSword) {
                attackId = this.mob.random.nextBoolean() ? 2 : 4;
                this.attackCooldown = (attackId == 2) ? 20 : 60;
            } else if (this.lastAttackId == 8 && this.mob.spellChoice == 3 && !hasSword) {
                attackId = 3;
                this.attackCooldown = 30;
            } else if (this.lastAttackId == 8 && this.mob.spellChoice == 0 && hasSword) {
                attackId = 8;
                this.mob.spellChoice = 2;
                this.attackCooldown = 20;
            } else if (this.lastAttackId == 8 && this.mob.spellChoice == 3 && hasSword) {
                attackId = 6;
                this.attackCooldown = 15;
            }

            if (attackId == this.lastAttackId) {
                this.consecutiveAttackCount++;
            } else {
                this.consecutiveAttackCount = 1;
                this.lastAttackId = attackId;
            }

            if (attackId == 2 || attackId == 4) {
                this.distantAttackCount++;
            } else {
                this.distantAttackCount = 0;
            }

            if (attackId != 0) {
                if (attackId == 8) {
                    this.mob.triggerSpell(this.mob.spellChoice);
                } else {
                    this.mob.setAttackState(attackId);
                }
            }
        }
    }
}