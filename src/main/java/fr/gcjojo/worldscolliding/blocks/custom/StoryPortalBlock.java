package fr.gcjojo.worldscolliding.blocks.custom;

import fr.gcjojo.worldscolliding.PlayerStoryDimensionData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumSet;
import java.util.Objects;

public class StoryPortalBlock extends Block {

    public StoryPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState p_54942_, BlockGetter p_54943_, BlockPos p_54944_, CollisionContext p_54945_) {
        return Block.box(0.0d, 0.0d, 0.0d, 16.0d, 16.0d, 16.0d);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos blockPos, Entity entity) {
        if(level.isClientSide()) return;

        if(entity instanceof ServerPlayer) {
            if (entity.canChangeDimensions()) {
                CompoundTag playerPersistentData = entity.getPersistentData();
                if(playerPersistentData.contains("StoryDimension"))
                {
                    PlayerStoryDimensionData playerData = PlayerStoryDimensionData.load(playerPersistentData.getCompound("StoryDimension"));

                    ServerLevel toLevel = switch (playerData.playerDimension) {
                        case "the_end" -> Objects.requireNonNull(entity.getServer()).getLevel(Level.END);
                        case "the_nether" -> Objects.requireNonNull(entity.getServer()).getLevel(Level.NETHER);
                        default -> Objects.requireNonNull(entity.getServer()).getLevel(Level.OVERWORLD);
                    };

                    entity.resetFallDistance();
                    entity.changeDimension(toLevel);
                    entity.teleportTo(toLevel, playerData.playerPos.x, playerData.playerPos.y, playerData.playerPos.z, EnumSet.noneOf(RelativeMovement.class), 0.0f, 0.0f);
                }
                //entity.teleportTo(entity.getServer().getLevel(level.OVERWORLD), entity.getX(), entity.getY(), entity.getZ(), Set.of(), 0.0f, 0.0f);
            }
        }
    }

    @Override
    public void randomTick(BlockState p_221799_, ServerLevel p_221800_, BlockPos p_221801_, RandomSource p_221802_) { }

    @Override
    public void animateTick(BlockState p_221794_, Level p_221795_, BlockPos p_221796_, RandomSource p_221797_) { }
}
