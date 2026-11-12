package factory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** World-wide record of how many stars each factory owner has earned. */
public final class FactoryData extends SavedData {
    private static final Codec<FactoryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).fieldOf("stars").forGetter(data -> data.stars)
    ).apply(instance, FactoryData::new));
    public static final SavedDataType<FactoryData> TYPE =
        new SavedDataType<>(Identifier.fromNamespaceAndPath("iuce", "factory"), FactoryData::new, CODEC);

    private final Map<UUID, Integer> stars;

    public FactoryData() { this(Map.of()); }

    private FactoryData(Map<UUID, Integer> stars) { this.stars = new HashMap<>(stars); }

    public static FactoryData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public int stars(UUID owner) { return stars.getOrDefault(owner, 0); }

    public void setStars(UUID owner, int value) {
        if (value <= 0) stars.remove(owner); else stars.put(owner, value);
        setDirty();
    }
}
