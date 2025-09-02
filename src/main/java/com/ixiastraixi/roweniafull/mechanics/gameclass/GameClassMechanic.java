package com.ixiastraixi.roweniafull.mechanics.gameclass;

import com.ixiastraixi.roweniafull.RoweniaFull;
import com.ixiastraixi.roweniafull.config.GameClassConfig;
import com.ixiastraixi.roweniafull.registry.gameclass.GameClassAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Minecraft 1.20.1, Forge 47.4.0
// Запрет использования блоков без нужного классового атрибута
public final class GameClassMechanic {

    // id блока → требуемый атрибут
    private final Map<ResourceLocation, RegistryObject<Attribute>> rules = new HashMap<>();
    private volatile boolean rulesBuilt = false;

    // конструктор без тяжёлой логики
    public GameClassMechanic() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    // слушаем загрузку/перезагрузку конфигов — перестраиваем правила
    @SubscribeEvent
    public void onConfigLoaded(ModConfigEvent.Loading e) {
        if (e.getConfig().getModId().equals(RoweniaFull.MOD_ID)) rebuildRules();
    }

    @SubscribeEvent
    public void onConfigReloaded(ModConfigEvent.Reloading e) {
        if (e.getConfig().getModId().equals(RoweniaFull.MOD_ID)) rebuildRules();
    }

    // безопасная перестройка правил
    private synchronized void rebuildRules() {
        rules.clear();
        map(GameClassConfig.warrior(),  GameClassAttributes.CLASS_WARRIOR);
        map(GameClassConfig.hunter(),   GameClassAttributes.CLASS_HUNTER);
        map(GameClassConfig.sorcerer(), GameClassAttributes.CLASS_SORCERER);
        map(GameClassConfig.wizard(),   GameClassAttributes.CLASS_WIZARD);
        map(GameClassConfig.cook(),     GameClassAttributes.CLASS_COOK);
        map(GameClassConfig.miner(),    GameClassAttributes.CLASS_MINER);
        rulesBuilt = true;
    }

    // ленивое построение, если кто‑то кликнул раньше событий конфига
    private void ensureRules() {
        if (!rulesBuilt) rebuildRules();
    }

    // маппинг списка id на атрибут с защитой
    private void map(List<? extends String> ids, RegistryObject<Attribute> attr) {
        if (ids == null || ids.isEmpty() || attr == null || !attr.isPresent()) return;
        for (String id : ids) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) rules.put(rl, attr);
        }
    }

    // обработчик клика ПКМ по блоку
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player == null || player.isSpectator()) return;

        ensureRules();

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        ResourceLocation blockId = state.getBlock().builtInRegistryHolder().key().location();
        RegistryObject<Attribute> requiredAttr = rules.get(blockId);
        if (requiredAttr == null) return;

        if (!hasClass(player, requiredAttr)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);

            String attrPath = requiredAttr.getId().getPath(); // class.warrior и т.д.
            Component className = Component.translatable("attribute.name." + RoweniaFull.MOD_ID + "." + attrPath);

            MutableComponent msg = Component.literal("[Rowenia] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("Вы не умеете пользоваться этим. ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("Необходим класс — ").withStyle(ChatFormatting.YELLOW))
                    .append(className.copy().withStyle(ChatFormatting.RED));

            player.displayClientMessage(msg, true);
        }
    }

    // проверка наличия класса (>= 1.0)
    private boolean hasClass(LivingEntity entity, RegistryObject<Attribute> attr) {
        if (entity == null || attr == null || !attr.isPresent()) return false;
        AttributeInstance inst = entity.getAttribute(attr.get());
        return inst != null && inst.getValue() >= 1.0D;
    }
}
