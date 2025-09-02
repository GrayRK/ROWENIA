package com.ixiastraixi.roweniafull.mechanics.deathItemloss;

import com.ixiastraixi.roweniafull.config.DeathItemLossConfig;
import com.ixiastraixi.roweniafull.registry.deathItemloss.DeathItemLossAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.*;
import java.util.stream.Collectors;

public class DeathItemLossMechanic {

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;

        RandomSource rng = player.getRandom();

        // Фильтры
        Set<ResourceLocation> whiteItems = parseRL(DeathItemLossConfig.WHITELIST_ITEMS.get());
        Set<ResourceLocation> blackItems = parseRL(DeathItemLossConfig.BLACKLIST_ITEMS.get());
        Set<TagKey<net.minecraft.world.item.Item>> whiteTags  = parseTags(DeathItemLossConfig.WHITELIST_TAGS.get());
        Set<TagKey<net.minecraft.world.item.Item>> blackTags  = parseTags(DeathItemLossConfig.BLACKLIST_TAGS.get());

        // Модификаторы по тегам
        List<TagChanceMod> tagMods = parseTagChanceModifiers(DeathItemLossConfig.TAG_CHANCE_MODIFIERS.get());

        // Сводка потерь
        Map<String, Integer> lost = new LinkedHashMap<>();

        // === VANILLA ===
        Inventory inv = player.getInventory();
        double hotbarChance   = clamp01(DeathItemLossConfig.HOTBAR_LOSS_CHANCE.get());
        double backpackChance = clamp01(DeathItemLossConfig.BACKPACK_LOSS_CHANCE.get());
        double offhandChance  = clamp01(DeathItemLossConfig.OFFHAND_LOSS_CHANCE.get());
        double helmetChance   = clamp01(DeathItemLossConfig.HELMET_LOSS_CHANCE.get());
        double chestChance    = clamp01(DeathItemLossConfig.CHEST_LOSS_CHANCE.get());
        double legsChance     = clamp01(DeathItemLossConfig.LEGS_LOSS_CHANCE.get());
        double bootsChance    = clamp01(DeathItemLossConfig.BOOTS_LOSS_CHANCE.get());

        // хотбар 0..8
        for (int i = 0; i <= 8; i++) {
            ItemStack s = inv.items.get(i);
            double eff = applyTagModifiers(hotbarChance, s, tagMods);
            eff = applyPlayerAttr(eff, player); // ГЛОБАЛЬНЫЙ атрибут (последним)
            if (shouldDelete(s, eff, rng, whiteItems, whiteTags, blackItems, blackTags)) {
                recordLoss(s, lost);
                inv.items.set(i, ItemStack.EMPTY);
            }
        }
        // рюкзак 9..35
        for (int i = 9; i <= 35; i++) {
            ItemStack s = inv.items.get(i);
            double eff = applyTagModifiers(backpackChance, s, tagMods);
            eff = applyPlayerAttr(eff, player);
            if (shouldDelete(s, eff, rng, whiteItems, whiteTags, blackItems, blackTags)) {
                recordLoss(s, lost);
                inv.items.set(i, ItemStack.EMPTY);
            }
        }
        // оффхэнд
        if (!inv.offhand.isEmpty()) {
            ItemStack off = inv.offhand.get(0);
            double eff = applyTagModifiers(offhandChance, off, tagMods);
            eff = applyPlayerAttr(eff, player);
            if (shouldDelete(off, eff, rng, whiteItems, whiteTags, blackItems, blackTags)) {
                recordLoss(off, lost);
                inv.offhand.set(0, ItemStack.EMPTY);
            }
        }
        // броня: 0 boots,1 legs,2 chest,3 head
        ItemStack boots = inv.armor.get(0);
        double eBoots = applyPlayerAttr(applyTagModifiers(bootsChance, boots, tagMods), player);
        if (shouldDelete(boots, eBoots, rng, whiteItems, whiteTags, blackItems, blackTags)) {
            recordLoss(boots, lost);
            inv.armor.set(0, ItemStack.EMPTY);
        }

        ItemStack legs = inv.armor.get(1);
        double eLegs = applyPlayerAttr(applyTagModifiers(legsChance, legs, tagMods), player);
        if (shouldDelete(legs, eLegs, rng, whiteItems, whiteTags, blackItems, blackTags)) {
            recordLoss(legs, lost);
            inv.armor.set(1, ItemStack.EMPTY);
        }

        ItemStack chest = inv.armor.get(2);
        double eChest = applyPlayerAttr(applyTagModifiers(chestChance, chest, tagMods), player);
        if (shouldDelete(chest, eChest, rng, whiteItems, whiteTags, blackItems, blackTags)) {
            recordLoss(chest, lost);
            inv.armor.set(2, ItemStack.EMPTY);
        }

        ItemStack helmet = inv.armor.get(3);
        double eHelmet = applyPlayerAttr(applyTagModifiers(helmetChance, helmet, tagMods), player);
        if (shouldDelete(helmet, eHelmet, rng, whiteItems, whiteTags, blackItems, blackTags)) {
            recordLoss(helmet, lost);
            inv.armor.set(3, ItemStack.EMPTY);
        }

        inv.setChanged();

        // === CURIOS ===
        if (ModList.get().isLoaded("curios")) {
            double curiosDefault = clampAllowNeg1(DeathItemLossConfig.CURIOS_DEFAULT_LOSS_CHANCE.get());
            Map<String, Double> overrides = parseCuriosOverrides(DeathItemLossConfig.CURIOS_SLOT_OVERRIDES.get());

            CuriosApi.getCuriosInventory(player).ifPresent(curiosInv -> {
                Map<String, ICurioStacksHandler> slotsMap = curiosInv.getCurios();
                slotsMap.forEach((slotIdRaw, handler) -> {
                    String slotId = slotIdRaw.toLowerCase(Locale.ROOT);
                    double baseChance = resolveCuriosChance(slotId, curiosDefault, overrides);
                    IDynamicStackHandler stacks = handler.getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        ItemStack s = stacks.getStackInSlot(i);
                        double eff = applyTagModifiers(baseChance, s, tagMods);
                        eff = applyPlayerAttr(eff, player); // глобальный множитель
                        if (shouldDelete(s, eff, rng, whiteItems, whiteTags, blackItems, blackTags)) {
                            recordLoss(s, lost);
                            stacks.setStackInSlot(i, ItemStack.EMPTY);
                        }
                    }
                });
            });
        }

        // === CHAT SUMMARY ===
        if (DeathItemLossConfig.CHAT_SHOW_SUMMARY.get() && !lost.isEmpty()) {
            sendLossSummary(player, lost, DeathItemLossConfig.CHAT_MAX_ENTRIES.get());
        }
    }

    // ─────────────── helpers ───────────────

    // Глобальный атрибут игрока (по умолчанию 1.0 = 100%)
    private static double applyPlayerAttr(double baseChance, ServerPlayer player) {
        double protection = 0.0;
        var inst = player.getAttribute(DeathItemLossAttributes.ITEM_LOSS_CHANCE.get());
        if (inst != null) protection = inst.getValue();
        if (protection < 0.0) protection = 0.0;
        if (protection > 1.0) protection = 1.0;
        return clamp01(baseChance * (1.0 - protection));
    }

    private static boolean shouldDelete(ItemStack stack,
                                        double chance,
                                        RandomSource rng,
                                        Set<ResourceLocation> whiteItems,
                                        Set<TagKey<net.minecraft.world.item.Item>> whiteTags,
                                        Set<ResourceLocation> blackItems,
                                        Set<TagKey<net.minecraft.world.item.Item>> blackTags) {
        if (stack == null || stack.isEmpty()) return false;
        if (isWhitelisted(stack, whiteItems, whiteTags)) return false; // белый список
        if (isBlacklisted(stack, blackItems, blackTags)) return true;  // чёрный список
        return rng.nextDouble() < clamp01(chance);
    }

    private static boolean isWhitelisted(ItemStack stack,
                                         Set<ResourceLocation> itemIds,
                                         Set<TagKey<net.minecraft.world.item.Item>> tags) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key != null && itemIds.contains(key)) return true;
        for (TagKey<net.minecraft.world.item.Item> tag : tags) if (stack.is(tag)) return true;
        return false;
    }

    private static boolean isBlacklisted(ItemStack stack,
                                         Set<ResourceLocation> itemIds,
                                         Set<TagKey<net.minecraft.world.item.Item>> tags) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key != null && itemIds.contains(key)) return true;
        for (TagKey<net.minecraft.world.item.Item> tag : tags) if (stack.is(tag)) return true;
        return false;
    }

    private static Set<ResourceLocation> parseRL(List<? extends String> raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptySet();
        Set<ResourceLocation> out = new HashSet<>();
        for (String s : raw) {
            try {
                ResourceLocation rl = ResourceLocation.tryParse(s.trim());
                if (rl != null) out.add(rl);
            } catch (Exception ignored) {}
        }
        return out;
    }

    private static Set<TagKey<net.minecraft.world.item.Item>> parseTags(List<? extends String> raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptySet();
        Set<TagKey<net.minecraft.world.item.Item>> out = new HashSet<>();
        for (String s : raw) {
            try {
                ResourceLocation rl = ResourceLocation.tryParse(s.trim());
                if (rl != null) out.add(TagKey.create(Registries.ITEM, rl));
            } catch (Exception ignored) {}
        }
        return out;
    }

    private static Map<String, Double> parseCuriosOverrides(List<? extends String> raw) {
        Map<String, Double> out = new HashMap<>();
        if (raw == null) return out;
        for (String line : raw) {
            int eq = line.indexOf('=');
            if (eq <= 0 || eq == line.length() - 1) continue;
            String key = line.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String val = line.substring(eq + 1).trim();
            try {
                double v = Double.parseDouble(val);
                out.put(key, v);
            } catch (NumberFormatException ignored) {}
        }
        return out;
    }

    private static double resolveCuriosChance(String slotId, double defaultChanceOrNeg1, Map<String, Double> overrides) {
        if (overrides.containsKey(slotId)) {
            double v = overrides.get(slotId);
            return (v < 0.0) ? clamp01(defaultChanceOrNeg1) : clamp01(v);
        }
        Double named = namedCuriosChance(slotId);
        if (named != null) {
            return (named < 0.0) ? clamp01(defaultChanceOrNeg1) : clamp01(named);
        }
        return clamp01(defaultChanceOrNeg1);
    }

    private static Double namedCuriosChance(String slotId) {
        switch (slotId) {
            case "ring":     return DeathItemLossConfig.CURIOS_RING_LOSS_CHANCE.get();
            case "necklace": return DeathItemLossConfig.CURIOS_NECKLACE_LOSS_CHANCE.get();
            case "belt":     return DeathItemLossConfig.CURIOS_BELT_LOSS_CHANCE.get();
            case "back":     return DeathItemLossConfig.CURIOS_BACK_LOSS_CHANCE.get();
            case "hands":    return DeathItemLossConfig.CURIOS_HANDS_LOSS_CHANCE.get();
            case "charm":    return DeathItemLossConfig.CURIOS_CHARM_LOSS_CHANCE.get();
            case "head":     return DeathItemLossConfig.CURIOS_HEAD_LOSS_CHANCE.get();
            case "body":     return DeathItemLossConfig.CURIOS_BODY_LOSS_CHANCE.get();
            default:         return null;
        }
    }

    // ── Tag modifiers ───────────────────────────────────────────

    private static final class TagChanceMod {
        final TagKey<net.minecraft.world.item.Item> tag;
        final double factor;
        TagChanceMod(TagKey<net.minecraft.world.item.Item> tag, double factor) { this.tag = tag; this.factor = factor; }
    }

    private static List<TagChanceMod> parseTagChanceModifiers(List<? extends String> raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        List<TagChanceMod> out = new ArrayList<>();
        for (String line : raw) {
            try {
                String[] parts = line.split("=", 2);
                if (parts.length != 2) continue;
                String left = parts[0].trim();   // namespace:tag
                String right = parts[1].trim();  // ±N%

                ResourceLocation rl = ResourceLocation.tryParse(left);
                if (rl == null) continue;

                String num = right.replace("%", "").replace("+", "").trim();
                double signed = Double.parseDouble(num);
                if (right.contains("-")) signed = -Math.abs(signed);

                double factor = 1.0 + (signed / 100.0);
                if (factor < 0.0) factor = 0.0;
                if (factor > 10.0) factor = 10.0;

                out.add(new TagChanceMod(TagKey.create(Registries.ITEM, rl), factor));
            } catch (Exception ignored) {}
        }
        return out;
    }

    private static double applyTagModifiers(double baseChance, ItemStack stack, List<TagChanceMod> mods) {
        if (stack == null || stack.isEmpty() || mods.isEmpty()) return clamp01(baseChance);
        double result = baseChance;
        for (TagChanceMod m : mods) {
            if (stack.is(m.tag)) result *= m.factor;
        }
        return clamp01(result);
    }

    // ── Chat summary ───────────────────────────────────────────

    private static void recordLoss(ItemStack stack, Map<String, Integer> lost) {
        if (stack == null || stack.isEmpty()) return;
        String name = stack.getHoverName().getString();
        lost.merge(name, stack.getCount(), Integer::sum);
    }

    private static void sendLossSummary(ServerPlayer player, Map<String, Integer> lost, int maxEntries) {
        int totalStacks = lost.values().stream().mapToInt(Integer::intValue).sum();

        List<Map.Entry<String, Integer>> entries = lost.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .collect(Collectors.toList());

        int shown = Math.min(maxEntries, entries.size());
        int hidden = entries.size() - shown;

        MutableComponent msg = Component.literal("[Rowenia] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Потеряно предметов: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(String.valueOf(totalStacks)).withStyle(ChatFormatting.RED));

        for (int i = 0; i < shown; i++) {
            var e = entries.get(i);
            msg = msg.append(Component.literal("\n").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("• ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(e.getKey()).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" ×" + e.getValue()).withStyle(ChatFormatting.GRAY));
        }
        if (hidden > 0) {
            msg = msg.append(Component.literal("\n+" + hidden + " ещё…").withStyle(ChatFormatting.DARK_GRAY));
        }
        player.sendSystemMessage(msg);
    }

    // utils
    private static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
    private static double clampAllowNeg1(double v) { return (v < 0 && v > -1.0000001) ? -1.0 : clamp01(v); }
}
