package com.ixiastraixi.roweniafull.mechanics.gameclass;

import com.ixiastraixi.roweniafull.RoweniaFull;
import com.ixiastraixi.roweniafull.registry.gameclass.GameClassAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.*;
import java.util.stream.Collectors;

// Minecraft 1.20.1, Forge 47.4.0
// Префикс [Класс1/Класс2/...] через Scoreboard Team — обновляется моментально везде
public final class GameClassNamePrefix {

    // порядок и цвета классов
    private static final LinkedHashMap<RegistryObject<Attribute>, ChatFormatting> CLASS_COLORS = new LinkedHashMap<>();
    static {
        CLASS_COLORS.put(GameClassAttributes.CLASS_WARRIOR, ChatFormatting.RED);
        CLASS_COLORS.put(GameClassAttributes.CLASS_HUNTER, ChatFormatting.GREEN);
        CLASS_COLORS.put(GameClassAttributes.CLASS_SORCERER, ChatFormatting.LIGHT_PURPLE);
        CLASS_COLORS.put(GameClassAttributes.CLASS_WIZARD, ChatFormatting.AQUA);
        CLASS_COLORS.put(GameClassAttributes.CLASS_COOK, ChatFormatting.GOLD);
        CLASS_COLORS.put(GameClassAttributes.CLASS_MINER, ChatFormatting.DARK_AQUA);
    }

    // кэш сигнатуры классов игрока, чтобы не трогать лишний раз
    private final Map<UUID, String> lastSig = new HashMap<>();

    // применяем/снимаем префикс при входе — чтобы сразу было корректно
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) applyOrClearPrefix(sp, classSignature(sp));
    }

    // снимаем игрока с команды при выходе (чистим мусор)
    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) removeFromTeam(sp);
    }

    // если игрок меняет мир/измерение — пересоберём префикс
    @SubscribeEvent
    public void onChangedDim(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) applyOrClearPrefix(sp, classSignature(sp));
    }

    // серверный тик: ловим изменение атрибутов и мгновенно обновляем префикс
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer sp)) return;
        String sig = classSignature(sp);
        UUID id = sp.getUUID();
        if (!Objects.equals(lastSig.get(id), sig)) {
            lastSig.put(id, sig);
            applyOrClearPrefix(sp, sig);
        }
    }

    // ——— внутрянка ———

    private void applyOrClearPrefix(ServerPlayer sp, String sig) {
        Scoreboard sb = sp.getScoreboard();
        String teamName = teamName(sp);
        PlayerTeam team = sb.getPlayerTeam(teamName);

        if (sig.isEmpty()) {
            // нет классов — снять префикс и убрать из команды
            if (team != null) {
                sb.removePlayerFromTeam(sp.getScoreboardName(), team);
                // можно удалить пустую команду, если никто не остался
                if (team.getPlayers().isEmpty()) {
                    sb.removePlayerTeam(team);
                }
            }
            return;
        }

        // есть классы — создаём/обновляем команду с нужным prefix
        if (team == null) team = sb.addPlayerTeam(teamName);
        team.setPlayerPrefix(buildPrefixComponent(sp)); // префикс вида "[К1/К2/...] "
        if (!team.getPlayers().contains(sp.getScoreboardName())) {
            sb.addPlayerToTeam(sp.getScoreboardName(), team);
        }
    }

    private void removeFromTeam(ServerPlayer sp) {
        Scoreboard sb = sp.getScoreboard();
        PlayerTeam team = sb.getPlayerTeam(teamName(sp));
        if (team != null) {
            sb.removePlayerFromTeam(sp.getScoreboardName(), team);
            if (team.getPlayers().isEmpty()) sb.removePlayerTeam(team);
        }
        lastSig.remove(sp.getUUID());
    }

    private String teamName(ServerPlayer sp) {
        // короткое и уникальное имя команды
        return "rowenia_cls_" + sp.getUUID().toString().substring(0, 8);
    }

    // строим текст префикса: "[Имя1/Имя2/... ] " (со скобками серым, классы — цветами)
    private MutableComponent buildPrefixComponent(Player p) {
        List<ClassInfo> classes = resolveClasses(p);
        MutableComponent out = Component.literal("[").withStyle(ChatFormatting.GRAY);
        boolean first = true;
        for (ClassInfo ci : classes) {
            if (!first) out.append(Component.literal("/").withStyle(ChatFormatting.GRAY));
            String path = ci.attr.getId().getPath(); // class.warrior
            Component className = Component.translatable("attribute.name." + RoweniaFull.MOD_ID + "." + path);
            out.append(className.copy().withStyle(ci.color));
            first = false;
        }
        return out.append(Component.literal("] ").withStyle(ChatFormatting.GRAY));
    }

    private List<ClassInfo> resolveClasses(Player p) {
        List<ClassInfo> res = new ArrayList<>();
        for (Map.Entry<RegistryObject<Attribute>, ChatFormatting> e : CLASS_COLORS.entrySet()) {
            RegistryObject<Attribute> ro = e.getKey();
            if (!ro.isPresent()) continue;
            AttributeInstance inst = p.getAttribute(ro.get());
            if (inst != null && inst.getValue() >= 1.0D) {
                res.add(new ClassInfo(ro, e.getValue()));
            }
        }
        return res;
    }

    private String classSignature(Player p) {
        return resolveClasses(p).stream()
                .map(ci -> ci.attr.getId().getPath())
                .collect(Collectors.joining("|")); // "class.warrior|class.miner" или ""
    }

    private record ClassInfo(RegistryObject<Attribute> attr, ChatFormatting color) {}
}
