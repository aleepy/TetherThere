package nulifie.xyz.tetherthere.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

public class ConfigUtil {
    public static Map<PotionEffectType, PotionEffect> loadDebuffEffects(ConfigurationSection debuffsSection) {
        Map<PotionEffectType, PotionEffect> debuffEffects = new HashMap<>();
        
        if (debuffsSection != null) {
            for (String key : debuffsSection.getKeys(false)) {
                ConfigurationSection effectSection = debuffsSection.getConfigurationSection(key);
                if (effectSection != null) {
                    PotionEffectType effectType = PotionEffectType.getByName(key.toUpperCase());
                    if (effectType != null) {
                        int duration = effectSection.getInt("duration_seconds") * 20;
                        int level = effectSection.getInt("level");
                        debuffEffects.put(effectType, new PotionEffect(effectType, duration, level));
                    }
                }
            }
        }
        
        return debuffEffects;
    }
} 