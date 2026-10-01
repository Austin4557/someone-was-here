package com.someonewashere.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public final class SomeoneWasHereClient implements ClientModInitializer {
    private final PresenceDirector director = new PresenceDirector();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                director.reset();
                return;
            }
            director.tick(client);
        });
    }

    static final class PresenceDirector {
        private int quietTicks = 20 * 75;

        void reset() {
            quietTicks = 20 * 75;
        }

        void tick(Minecraft client) {
            if (--quietTicks > 0) return;

            ThreadLocalRandom rng = ThreadLocalRandom.current();
            int roll = rng.nextInt(100);
            boolean happened = roll < 34 ? tryDoorEvent(client, rng)
                    : roll < 58 ? tryTorchEvent(client, rng)
                    : roll < 78 ? phantomContainerEvent(client, rng)
                    : footstepsAboveEvent(client, rng);

            // Failed searches stay quiet too. The mod should never feel scheduled.
            quietTicks = happened
                    ? rng.nextInt(20 * 80, 20 * 241)
                    : rng.nextInt(20 * 25, 20 * 71);
        }

        private boolean tryDoorEvent(Minecraft client, ThreadLocalRandom rng) {
            List<BlockPos> doors = nearby(client, 9, state -> state.getBlock() instanceof DoorBlock);
            if (doors.isEmpty()) return false;
            BlockPos pos = doors.get(rng.nextInt(doors.size()));
            BlockState state = client.level.getBlockState(pos);
            boolean open = state.getValue(BlockStateProperties.OPEN);
            // Client-side prediction only: no server/world save mutation.
            client.level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, !open), 2);
            client.level.playLocalSound(pos, open ? SoundEvents.WOODEN_DOOR_CLOSE : SoundEvents.WOODEN_DOOR_OPEN,
                    SoundSource.BLOCKS, 0.55F, 0.92F + rng.nextFloat() * 0.12F, false);
            return true;
        }

        private boolean tryTorchEvent(Minecraft client, ThreadLocalRandom rng) {
            List<BlockPos> torches = nearby(client, 8, state -> state.is(net.minecraft.tags.BlockTags.WALL_POST_OVERRIDE));
            if (torches.isEmpty()) return false;
            BlockPos pos = torches.get(rng.nextInt(torches.size()));
            // No block replacement: sell the illusion with a tiny extinguish cue near an existing light source.
            client.level.playLocalSound(pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.AMBIENT,
                    0.18F, 0.72F + rng.nextFloat() * 0.18F, false);
            return true;
        }

        private boolean footstepsAboveEvent(Minecraft client, ThreadLocalRandom rng) {
            Vec3 p = client.player.position();
            double angle = rng.nextDouble(Math.PI * 2.0);
            double lateral = rng.nextDouble(1.5, 4.5);
            int steps = rng.nextInt(3, 7);
            double dx = Math.sin(angle);
            double dz = Math.cos(angle);
            for (int i = 0; i < steps; i++) {
                double stride = (i - (steps - 1) * 0.5) * 0.72;
                client.level.playLocalSound(
                        p.x + dx * lateral + dz * stride,
                        p.y + rng.nextDouble(3.0, 5.5),
                        p.z + dz * lateral - dx * stride,
                        SoundEvents.WOOD_STEP, SoundSource.AMBIENT,
                        0.34F, 0.82F + rng.nextFloat() * 0.16F, false);
            }
            return true;
        }

        private boolean phantomContainerEvent(Minecraft client, ThreadLocalRandom rng) {
            Vec3 p = client.player.position();
            double angle = rng.nextDouble(Math.PI * 2.0);
            double distance = rng.nextDouble(4.0, 10.0);
            client.level.playLocalSound(
                    p.x + Math.sin(angle) * distance, p.y + rng.nextDouble(-1.0, 1.5),
                    p.z + Math.cos(angle) * distance,
                    rng.nextBoolean() ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE,
                    SoundSource.BLOCKS, 0.42F, 0.88F + rng.nextFloat() * 0.16F, false);
            return true;
        }

        private List<BlockPos> nearby(Minecraft client, int radius, java.util.function.Predicate<BlockState> predicate) {
            BlockPos origin = client.player.blockPosition();
            List<BlockPos> found = new ArrayList<>();
            for (int x = -radius; x <= radius; x++) {
                for (int y = -4; y <= 4; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = origin.offset(x, y, z);
                        if (predicate.test(client.level.getBlockState(pos))) found.add(pos.immutable());
                    }
                }
            }
            Collections.shuffle(found);
            return found;
        }
    }
}
