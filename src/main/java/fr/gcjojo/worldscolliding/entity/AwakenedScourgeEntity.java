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
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import fr.gcjojo.worldscolliding.ModSounds;

import java.util.EnumSet;
import java.util.List;

public class AwakenedScourgeEntity extends Monster implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(AwakenedScourgeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HAS_SWORD = SynchedEntityData.defineId(AwakenedScourgeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SPAWNED = SynchedEntityData.defineId(AwakenedScourgeEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = (ServerBossEvent)(new ServerBossEvent(Component.literal("The Awakened Scourge"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS)).setDarkenScreen(true);

    public int attackTick = 0;
    private int spellChoice = 0;

    public AwakenedScourgeEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.7D)
                .add(Attributes.ATTACK_DAMAGE, 25.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FLYING_SPEED, 0.7D);
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
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, 1);
        this.entityData.define(HAS_SWORD, false);
        this.entityData.define(SPAWNED, false);
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
                case 11: return event.setAndContinue(RawAnimation.begin().thenPlay("shield"));
                default:
                    if (event.isMoving()) {
                        return event.setAndContinue(RawAnimation.begin().thenLoop(hasSword ? "iddle_sword" : "iddle"));
                    }
                    return event.setAndContinue(RawAnimation.begin().thenLoop(hasSword ? "iddle_sword" : "iddle"));
            }
        }).setSoundKeyframeHandler(event -> {
            String sound = event.getKeyframeData().getSound();
            if (sound.contains("sword_draw")) {
                this.playSound(ModSounds.SWORD_DRAW.get(), 1.0f, 1.0f);
            } else if (sound.contains("protoss")) {
                this.playSound(ModSounds.PROTOSS_ELECTRIC.get(), 1.0f, 1.0f);
            } else if (sound.contains("master_sword") || sound.contains("zeldamastersword")) {
                this.playSound(ModSounds.MASTER_SWORD.get(), 1.0f, 1.0f);
            } else if (sound.contains("enterganondorf")) {
                this.playSound(ModSounds.ENTER_GANONDORF.get(), 1.0f, 1.0f);
            } else if (sound.contains("laugh")) {
                this.playSound(ModSounds.LAUGH.get(), 1.0f, 1.0f);
            } else if (sound.contains("switchclick")) {
                this.playSound(ModSounds.SWITCH_CLICK.get(), 1.0f, 1.0f);
            }
        }));
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

        if (this.level().isClientSide && this.entityData.get(SPAWNED)) {
            double px = this.getX() + (this.random.nextDouble() - 0.5) * 1.5;
            double py = this.getY() + 0.1;
            double pz = this.getZ() + (this.random.nextDouble() - 0.5) * 1.5;
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, px, py, pz, 0, 0.01, 0);
        }

        if (!this.level().isClientSide) {
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

            int currentState = this.entityData.get(STATE);

            if (currentState == 1) {
                this.attackTick++;
                if (this.attackTick == 400) {
                    AABB aabb = new AABB(this.blockPosition()).inflate(50);
                    List<ServerPlayer> players = this.level().getEntitiesOfClass(ServerPlayer.class, aabb);
                    for (ServerPlayer p : players) {
                        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
                        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4The Scourge")));
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
                    Vec3 start = this.position().add(0, this.getBbHeight() * 0.5, 0);
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
                    AABB hitBox = new AABB(this.getX() - 10, this.getY() - 10, this.getZ() - 10, this.getX() + 10, this.getY() + 10, this.getZ() + 10);
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
                            Vec3 pushVector = dir.scale(-2.0);
                            this.getTarget().setDeltaMovement(pushVector);
                            this.getTarget().hurtMarked = true;
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
                            Vec3 pullVector = dir.scale(2.5);
                            this.getTarget().setDeltaMovement(pullVector);
                            this.getTarget().hurtMarked = true;
                        }
                    }
                }
                if (this.attackTick >= 40) {
                    if (this.spellChoice == 2) {
                        this.setAttackState(this.entityData.get(HAS_SWORD) ? 6 : 3);
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
                if (this.attackTick % 10 == 0) {
                    Vec3 dir = new Vec3(this.random.nextDouble() - 0.5, this.random.nextDouble() - 0.5, this.random.nextDouble() - 0.5).normalize();
                    Vec3 start = this.position().add(0, this.getBbHeight() * 0.5, 0);
                    for (int i = 1; i < 20; i++) {
                        Vec3 pos = start.add(dir.scale(i));
                        if (!this.level().getBlockState(new BlockPos((int)pos.x, (int)pos.y, (int)pos.z)).isAir()) {
                            break;
                        }
                        ((ServerLevel)this.level()).sendParticles(DustParticleOptions.REDSTONE, pos.x, pos.y, pos.z, 2, 0.1, 0.1, 0.1, 0.0);
                        AABB hitBox = new AABB(pos.x - 1, pos.y - 1, pos.z - 1, pos.x + 1, pos.y + 1, pos.z + 1);
                        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != this);
                        for (LivingEntity target : targets) {
                            target.hurt(this.damageSources().magic(), 10.0f);
                        }
                    }
                }
                if (this.attackTick >= 60) {
                    this.setAttackState(0);
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
            List<Phantom> allies = this.mob.level().getEntitiesOfClass(Phantom.class, this.mob.getBoundingBox().inflate(15));

            if (!allies.isEmpty() && this.mob.random.nextFloat() < 0.2f) {
                attackId = 11;
                this.attackCooldown = 80;
            } else if (isBehind) {
                attackId = hasSword ? 10 : 9;
                this.attackCooldown = 40;
            } else if (!hasSword) {
                if (distance < 25.0) {
                    attackId = 3;
                    this.attackCooldown = 30;
                } else {
                    if (this.mob.random.nextFloat() < 0.4f) {
                        attackId = 4;
                        this.attackCooldown = 60;
                    } else if (this.mob.random.nextFloat() < 0.4f) {
                        attackId = 8;
                        int[] spells = {0, 1, 3};
                        this.mob.spellChoice = spells[this.mob.random.nextInt(spells.length)];
                        this.attackCooldown = 40;
                    } else {
                        attackId = 2;
                        this.attackCooldown = 20;
                    }
                }
            } else {
                if (distance < 25.0) {
                    if (this.mob.random.nextFloat() < 0.6f) {
                        attackId = 6;
                        this.attackCooldown = 15;
                    } else {
                        attackId = 8;
                        int[] spells = {0, 1, 3};
                        this.mob.spellChoice = spells[this.mob.random.nextInt(spells.length)];
                        this.attackCooldown = 40;
                    }
                } else if (distance >= 25.0 && distance < 100.0) {
                    attackId = 7;
                    this.attackCooldown = 100;
                } else {
                    attackId = 8;
                    this.mob.spellChoice = 2;
                    this.attackCooldown = 20;
                }
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