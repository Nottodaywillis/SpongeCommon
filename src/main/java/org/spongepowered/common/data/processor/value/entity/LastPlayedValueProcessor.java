/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.data.processor.value.entity;

import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.common.data.processor.common.AbstractSpongeValueProcessor;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;
import org.spongepowered.common.entity.player.SpongeUser;
import org.spongepowered.common.world.storage.SpongePlayerDataHandler;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class LastPlayedValueProcessor extends AbstractSpongeValueProcessor<Identifiable, Instant, Value<Instant>> {

    public LastPlayedValueProcessor() {
        super(Identifiable.class, Keys.LAST_DATE_PLAYED);
    }

    @Override
    protected boolean supports(Identifiable dataHolder) {
        return dataHolder instanceof EntityPlayer || dataHolder instanceof SpongeUser;
    }

    @Override
    protected Value<Instant> constructValue(Instant actualValue) {
        return new SpongeValue<>(Keys.LAST_DATE_PLAYED, Instant.now(), actualValue);
    }

    @Override
    protected boolean set(Identifiable container, Instant value) {
        final UUID id = container.getUniqueId();
        final Instant played = SpongePlayerDataHandler.getFirstJoined(id).get();
        SpongePlayerDataHandler.setPlayerInfo(id, played, value);
        return true;
    }

    @Override
    protected Optional<Instant> getVal(Identifiable container) {
        return SpongePlayerDataHandler.getLastPlayed(container.getUniqueId());
    }

    @Override
    protected ImmutableValue<Instant> constructImmutableValue(Instant value) {
        return new ImmutableSpongeValue<Instant>(Keys.LAST_DATE_PLAYED, Instant.now(), value);
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }
}
