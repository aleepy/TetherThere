package nulifie.xyz.tetherthere.util;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public class EffectUtil {
    public static void applyDebuffs(Player player, Map<PotionEffectType, PotionEffect> debuffEffects) {
        for (PotionEffect effect : debuffEffects.values()) {
            player.addPotionEffect(effect);
        }
        player.setCollidable(false);
    }

    public static void removeDebuffs(Player player, Map<PotionEffectType, PotionEffect> debuffEffects) {
        for (PotionEffectType effectType : debuffEffects.keySet()) {
            player.removePotionEffect(effectType);
        }
        player.setCollidable(true);
    }
} 