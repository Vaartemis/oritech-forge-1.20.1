package rearth.oritech.port;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.fluid.FluidStack;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static rearth.oritech.port.RegistryFriendlyByteBuf.friendly;

/** Codecs that 1.21 exposed as static fields on vanilla classes (PortCodecs.RESOURCE_LOCATION etc.). */
public final class PortCodecs {
    private PortCodecs() { }

    public static final StreamCodec<ByteBuf, ResourceLocation> RESOURCE_LOCATION = StreamCodec.of((b, v) -> friendly(b).writeResourceLocation(v), b -> friendly(b).readResourceLocation());
    public static final StreamCodec<ByteBuf, BlockPos> BLOCK_POS = StreamCodec.of((b, v) -> friendly(b).writeBlockPos(v), b -> friendly(b).readBlockPos());
    public static final StreamCodec<ByteBuf, java.util.UUID> UUID = StreamCodec.of((b, v) -> friendly(b).writeUUID(v), b -> friendly(b).readUUID());
    public static final StreamCodec<ByteBuf, ItemStack> ITEM_STACK = StreamCodec.of((b, v) -> friendly(b).writeItem(v), b -> friendly(b).readItem());
    public static final StreamCodec<ByteBuf, List<ItemStack>> ITEM_STACK_LIST = ITEM_STACK.apply(ByteBufCodecs.list());
    public static final StreamCodec<ByteBuf, Ingredient> INGREDIENT = StreamCodec.of((b, v) -> v.toNetwork(friendly(b)), b -> Ingredient.fromNetwork(friendly(b)));
    public static final StreamCodec<ByteBuf, FluidStack> FLUID_STACK = StreamCodec.of((b, v) -> v.write(friendly(b)), b -> FluidStack.read(friendly(b)));

    // ---- DFU codecs ----

    /** 1.21 style item stack: {"id": "...", "count": n} (also accepts "item" / "Count", optional "nbt"). */
    public static final Codec<ItemStack> ITEM_STACK_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<ItemStack, T>> decode(DynamicOps<T> ops, T input) {
            var dyn = new Dynamic<>(ops, input);
            var asString = dyn.asString().result();
            if (asString.isPresent()) {
                var item = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(asString.get()));
                return item.map(i -> DataResult.success(Pair.of(new ItemStack(i), input))).orElseGet(() -> DataResult.error(() -> "unknown item " + asString.get()));
            }
            var id = dyn.get("id").asString().result().or(() -> dyn.get("item").asString().result());
            if (id.isEmpty()) return DataResult.error(() -> "item stack without id");
            var item = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(id.get()));
            if (item.isEmpty()) return DataResult.error(() -> "unknown item " + id.get());
            int count = dyn.get("count").asInt(dyn.get("Count").asInt(1));
            var stack = new ItemStack(item.get(), count);
            dyn.get("nbt").result().ifPresent(n -> {
                var tag = n.convert(net.minecraft.nbt.NbtOps.INSTANCE).getValue();
                if (tag instanceof CompoundTag c) stack.setTag(c);
            });
            return DataResult.success(Pair.of(stack, input));
        }

        @Override
        public <T> DataResult<T> encode(ItemStack stack, DynamicOps<T> ops, T prefix) {
            var map = ops.mapBuilder();
            map.add("id", ops.createString(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
            if (stack.getCount() != 1) map.add("count", ops.createInt(stack.getCount()));
            if (stack.hasTag()) map.add("nbt", net.minecraft.nbt.NbtOps.INSTANCE.convertTo(ops, stack.getTag()));
            return map.build(prefix);
        }
    };

    public static final Codec<ItemStack> OPTIONAL_ITEM_STACK_CODEC = ITEM_STACK_CODEC;

    /** Ingredient through its JSON form. */
    public static final Codec<Ingredient> INGREDIENT_CODEC = Codec.PASSTHROUGH.comapFlatMap(
      dyn -> {
          try {
              JsonElement json = dyn.convert(JsonOps.INSTANCE).getValue();
              return DataResult.success(Ingredient.fromJson(json, false));
          } catch (Exception e) {
              return DataResult.error(e::getMessage);
          }
      },
      ing -> new Dynamic<>(JsonOps.INSTANCE, ing.toJson()));

    public static final Codec<FluidStack> FLUID_STACK_CODEC = RecordCodecBuilder.create(i -> i.group(
      ResourceLocation.CODEC.fieldOf("id").forGetter(f -> BuiltInRegistries.FLUID.getKey(f.getFluid())),
      Codec.LONG.optionalFieldOf("amount", 1000L).forGetter(FluidStack::getAmount),
      CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(f -> Optional.ofNullable(f.getTag()))
    ).apply(i, (id, amount, tag) -> {
        var fluid = BuiltInRegistries.FLUID.get(id);
        if (fluid == Fluids.EMPTY) return FluidStack.empty();
        return FluidStack.create(fluid, amount, tag.orElse(null));
    }));

    public static final Codec<FluidStack> OPTIONAL_FLUID_STACK_CODEC = FLUID_STACK_CODEC;
}
