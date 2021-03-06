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
package org.spongepowered.common.mixin.core.entity.boss.dragon.phase;

import com.flowpowered.math.vector.Vector3d;
import net.minecraft.entity.boss.dragon.phase.IPhase;
import net.minecraft.entity.boss.dragon.phase.PhaseList;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.api.entity.living.complex.dragon.phase.EnderDragonPhase;
import org.spongepowered.api.entity.living.complex.dragon.phase.EnderDragonPhaseType;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.util.VecHelper;

import java.util.Optional;

@Mixin(IPhase.class)
@Implements(@Interface(iface = EnderDragonPhase.class, prefix = "phase$"))
public interface MixinIPhase {

    @Shadow Vec3d getTargetLocation();
    @Shadow PhaseList<? extends IPhase> getType();

    default EnderDragonPhaseType phase$getType() {
        return (EnderDragonPhaseType) getType();
    }

    default Optional<Vector3d> phase$getTargetPosition() {
        final Vec3d vec = this.getTargetLocation();
        return vec == null ? Optional.empty() : Optional.of(VecHelper.toVector3d(vec));
    }

}
