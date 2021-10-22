package me.william278.crossserversync.bukkit;

import me.william278.crossserversync.redis.RedisMessage;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class for serializing and deserializing player inventories and Ender Chests contents ({@link ItemStack[]}) as base64 strings.
 * Based on https://gist.github.com/graywolf336/8153678 by graywolf336
 * Modified for 1.16 via https://gist.github.com/graywolf336/8153678#gistcomment-3551376 by efindus
 *
 * @author efindus
 * @author graywolf336
 * @author William278
 */
public final class DataSerializer {

    /**
     * Converts the player inventory to a Base64 encoded string.
     *
     * @param player whose inventory will be turned into an array of strings.
     * @return string with serialized Inventory
     * @throws IllegalStateException in the event the item stacks cannot be saved
     */
    public static String getSerializedInventoryContents(Player player) throws IllegalStateException {
        // This contains contents, armor and offhand (contents are indexes 0 - 35, armor 36 - 39, offhand - 40)
        return itemStackArrayToBase64(player.getInventory().getContents());
    }

    /**
     * Converts the player inventory to a Base64 encoded string.
     *
     * @param player whose Ender Chest will be turned into an array of strings.
     * @return string with serialized Ender Chest
     * @throws IllegalStateException in the event the item stacks cannot be saved
     */
    public static String getSerializedEnderChestContents(Player player) throws IllegalStateException {
        // This contains all slots (0-27) in the player's Ender Chest
        return itemStackArrayToBase64(player.getEnderChest().getContents());
    }

    public static String getSerializedEffectData(Player player) {
        PotionEffect[] potionEffects = new PotionEffect[player.getActivePotionEffects().size()];
        int x = 0;
        for (PotionEffect effect : player.getActivePotionEffects()) {
            potionEffects[x] = effect;
            x++;
        }
        return effectArrayToBase64(potionEffects);
    }

    public static String effectArrayToBase64(PotionEffect[] effects) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
                dataOutput.writeInt(effects.length);

                for (PotionEffect effect : effects) {
                    if (effect != null) {
                        dataOutput.writeObject(effect.serialize());
                    } else {
                        dataOutput.writeObject(null);
                    }
                }
            }
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save potion effects.", e);
        }
    }

    /**
     * A method to serialize an {@link ItemStack} array to Base64 String.
     *
     * @param items to turn into a Base64 String.
     * @return Base64 string of the items.
     * @throws IllegalStateException in the event the item stacks cannot be saved
     */
    public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
                dataOutput.writeInt(items.length);

                for (ItemStack item : items) {
                    if (item != null) {
                        dataOutput.writeObject(item.serialize());
                    } else {
                        dataOutput.writeObject(null);
                    }
                }
            }
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    /**
     * Gets an array of ItemStacks from a Base64 string.
     *
     * @param data Base64 string to convert to ItemStack array.
     * @return ItemStack array created from the Base64 string.
     * @throws IOException in the event the class type cannot be decoded
     */
    public static ItemStack[] itemStackArrayFromBase64(String data) throws IOException {
        // Return an empty ItemStack[] if the data is empty
        if (data.isEmpty()) {
            return new ItemStack[0];
        }
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data))) {
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];

            for (int Index = 0; Index < items.length; Index++) {
                @SuppressWarnings("unchecked") // Ignore the unchecked cast here
                Map<String, Object> stack = (Map<String, Object>) dataInput.readObject();

                if (stack != null) {
                    items[Index] = ItemStack.deserialize(stack);
                } else {
                    items[Index] = null;
                }
            }

            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

    public static PotionEffect[] potionEffectArrayFromBase64(String data) throws IOException {
        // Return an empty PotionEffect[] if the data is empty
        if (data.isEmpty()) {
            return new PotionEffect[0];
        }
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data))) {
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            PotionEffect[] items = new PotionEffect[dataInput.readInt()];

            for (int Index = 0; Index < items.length; Index++) {
                @SuppressWarnings("unchecked") // Ignore the unchecked cast here
                Map<String, Object> effect = (Map<String, Object>) dataInput.readObject();

                if (effect != null) {
                    items[Index] = new PotionEffect(effect);
                } else {
                    items[Index] = null;
                }
            }

            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

    public static StatisticData deserializeStatisticData(String serializedStatisticData) throws IOException {
        if (serializedStatisticData.isEmpty()) {
            return new StatisticData(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
        }
        try {
            return (StatisticData) RedisMessage.deserialize(serializedStatisticData);
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

    public static String getSerializedStatisticData(Player player) throws IOException {
        HashMap<Statistic,Integer> untypedStatisticValues = new HashMap<>();
        HashMap<Statistic,HashMap<Material,Integer>> blockStatisticValues = new HashMap<>();
        HashMap<Statistic,HashMap<Material,Integer>> itemStatisticValues = new HashMap<>();
        HashMap<Statistic,HashMap<EntityType,Integer>> entityStatisticValues = new HashMap<>();
        for (Statistic statistic : Statistic.values()) {
            switch (statistic.getType()) {
                case ITEM -> {
                    HashMap<Material,Integer> itemValues = new HashMap<>();
                    for (Material itemMaterial : Arrays.stream(Material.values()).filter(Material::isItem).collect(Collectors.toList())) {
                        itemValues.put(itemMaterial, player.getStatistic(statistic, itemMaterial));
                    }
                    itemStatisticValues.put(statistic, itemValues);
                }
                case BLOCK -> {
                    HashMap<Material,Integer> blockValues = new HashMap<>();
                    for (Material blockMaterial : Arrays.stream(Material.values()).filter(Material::isBlock).collect(Collectors.toList())) {
                        blockValues.put(blockMaterial, player.getStatistic(statistic, blockMaterial));
                    }
                    blockStatisticValues.put(statistic, blockValues);
                }
                case ENTITY -> {
                    HashMap<EntityType,Integer> entityValues = new HashMap<>();
                    for (EntityType type : Arrays.stream(EntityType.values()).filter(EntityType::isAlive).collect(Collectors.toList())) {
                        entityValues.put(type, player.getStatistic(statistic, type));
                    }
                    entityStatisticValues.put(statistic, entityValues);
                }
                case UNTYPED -> untypedStatisticValues.put(statistic, player.getStatistic(statistic));
            }
        }

        StatisticData statisticData = new StatisticData(untypedStatisticValues, blockStatisticValues, itemStatisticValues, entityStatisticValues);
        return RedisMessage.serialize(statisticData);
    }

    public record StatisticData(HashMap<Statistic,Integer> untypedStatisticValues,
                                HashMap<Statistic,HashMap<Material,Integer>> blockStatisticValues,
                                HashMap<Statistic,HashMap<Material,Integer>> itemStatisticValues,
                                HashMap<Statistic,HashMap<EntityType,Integer>> entityStatisticValues) implements Serializable { }
}