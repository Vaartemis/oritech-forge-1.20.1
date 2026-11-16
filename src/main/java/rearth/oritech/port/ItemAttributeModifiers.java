package rearth.oritech.port;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 1.21 ItemAttributeModifiers emulation; applied through Forge ItemAttributeModifierEvent. */
public record ItemAttributeModifiers(List<Entry> modifiers) {

    public static final ItemAttributeModifiers EMPTY = new ItemAttributeModifiers(List.of());
    public static final Map<Item, ItemAttributeModifiers> BY_ITEM = new IdentityHashMap<>();

    public record Entry(java.util.function.Supplier<? extends Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {}

    public static Builder builder() { return new Builder(); }

    public ItemAttributeModifiers withModifierAdded(Attribute attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
        return withModifierAdded(() -> attribute, modifier, slot);
    }

    public ItemAttributeModifiers withModifierAdded(java.util.function.Supplier<? extends Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
        var list = new ArrayList<>(modifiers);
        list.add(new Entry(attribute, modifier, slot));
        return new ItemAttributeModifiers(ImmutableList.copyOf(list));
    }

    public Multimap<Attribute, AttributeModifier> forSlot(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> map = HashMultimap.create();
        for (var e : modifiers) if (e.slot.test(slot)) map.put(e.attribute.get(), e.modifier);
        return map;
    }

    public static <T extends Item> T attach(T item, ItemAttributeModifiers mods) {
        BY_ITEM.put(item, mods);
        return item;
    }

    public static UUID uuidOf(ResourceLocation id) {
        return UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static AttributeModifier mod(ResourceLocation id, double amount, AttributeModifier.Operation op) {
        return new AttributeModifier(uuidOf(id), id.toString(), amount, op);
    }

    public static AttributeModifier mod(UUID id, double amount, AttributeModifier.Operation op) {
        return new AttributeModifier(id, "oritech", amount, op);
    }

    public static final class Builder {
        private final List<Entry> list = new ArrayList<>();
        public Builder add(Attribute attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
            return add(() -> attribute, modifier, slot);
        }
        public Builder add(java.util.function.Supplier<? extends Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
            list.add(new Entry(attribute, modifier, slot));
            return this;
        }
        public ItemAttributeModifiers build() { return new ItemAttributeModifiers(ImmutableList.copyOf(list)); }
    }
}
