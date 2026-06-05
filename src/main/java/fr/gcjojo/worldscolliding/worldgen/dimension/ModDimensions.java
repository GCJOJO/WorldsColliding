package fr.gcjojo.worldscolliding.worldgen.dimension;

import fr.gcjojo.worldscolliding.ModEntry;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

import java.util.ArrayList;
import java.util.Optional;
import java.util.OptionalLong;

public class ModDimensions
{
    public static final ResourceKey<LevelStem> STORY_DIM_KEY = ResourceKey.create(Registries.LEVEL_STEM,
            ResourceLocation.fromNamespaceAndPath(ModEntry.MODID, "story"));
    public static final ResourceKey<Level> STORY_DIM_LEVEL_KEY = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(ModEntry.MODID, "story"));
    public static final ResourceKey<DimensionType> STORY_DIM_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(ModEntry.MODID, "story_type"));


    public static void bootstrapType(BootstapContext<DimensionType> context)
    {
        context.register(STORY_DIM_TYPE, new DimensionType(
                OptionalLong.of(12000),
                false,
                false,
                false,
                false,
                1.0,
                true,
                true,
                0,
                256,
                256,
                BlockTags.INFINIBURN_OVERWORLD,
                BuiltinDimensionTypes.OVERWORLD_EFFECTS,
                1.0f,
                new DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0)
        ));
    }

    public static void bootstrapStem(BootstapContext<LevelStem> context)
    {
        HolderGetter<Biome> biomeRegistry = context.lookup(Registries.BIOME);
        HolderGetter<DimensionType> dimTypes = context.lookup(Registries.DIMENSION_TYPE);

        FlatLevelGeneratorSettings flatLevelGenSettings = new FlatLevelGeneratorSettings(Optional.empty(), biomeRegistry.getOrThrow(Biomes.THE_VOID), new ArrayList<>());
        flatLevelGenSettings.getLayersInfo().add(new FlatLayerInfo(1, Blocks.BEDROCK));

        FlatLevelSource chunkGenerator = new FlatLevelSource(flatLevelGenSettings);

        LevelStem stem = new LevelStem(dimTypes.getOrThrow(ModDimensions.STORY_DIM_TYPE), chunkGenerator);
        context.register(STORY_DIM_KEY, stem);
    }
}
