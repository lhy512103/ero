package item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** The guide book item. Right-click opens the built-in guide; the client installs the opener. */
public class GuideItem extends Item {
    /** Receives the page id to open (null = last viewed page). A no-op on dedicated servers. */
    public static Consumer<String> opener = pageId -> { };

    public GuideItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) opener.accept(null);
        return InteractionResult.SUCCESS;
    }
}
