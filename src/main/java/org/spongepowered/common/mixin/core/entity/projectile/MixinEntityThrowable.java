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
package org.spongepowered.common.mixin.core.entity.projectile;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.projectile.source.ProjectileSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.entity.projectile.ProjectileSourceSerializer;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.mixin.core.entity.MixinEntity;

import javax.annotation.Nullable;

@Mixin(EntityThrowable.class)
public abstract class MixinEntityThrowable extends MixinEntity implements Projectile {

    @Shadow private EntityLivingBase thrower;
    @Shadow private String throwerName;
    @Shadow public abstract EntityLivingBase getThrower();
    @Shadow protected abstract void onImpact(RayTraceResult movingObjectPosition);


    @Nullable
    public ProjectileSource projectileSource;

    @Override
    public ProjectileSource getShooter() {
        if (this.projectileSource != null) {
            return this.projectileSource;
        } else if (this.getThrower() != null && this.getThrower() instanceof ProjectileSource) {
            return (ProjectileSource) this.getThrower();
        }

        return ProjectileSource.UNKNOWN;
    }

    @Override
    public void setShooter(ProjectileSource shooter) {
        if (shooter instanceof EntityLivingBase) {
            // This allows things like Vanilla kill attribution to take place
            this.thrower = (EntityLivingBase) shooter;
        } else {
            this.thrower = null;
        }

        this.throwerName = null;
        this.projectileSource = shooter;
    }

    @Override
    public void readFromNbt(NBTTagCompound compound) {
        super.readFromNbt(compound);
        ProjectileSourceSerializer.readSourceFromNbt(compound, this);
    }

    @Override
    public void writeToNbt(NBTTagCompound compound) {
        super.writeToNbt(compound);
        ProjectileSourceSerializer.writeSourceToNbt(compound, this.getShooter(), this.thrower);
    }

    @Redirect(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/EntityThrowable;onImpact(Lnet/minecraft/util/math/RayTraceResult;)V"))
    public void onProjectileImpact(EntityThrowable projectile, RayTraceResult movingObjectPosition) {
        if (this.world.isRemote || movingObjectPosition.typeOfHit == RayTraceResult.Type.MISS) {
            this.onImpact(movingObjectPosition);
            return;
        }

        if (!SpongeCommonEventFactory.handleCollideImpactEvent(projectile, this.getShooter(), movingObjectPosition)) {
            this.onImpact(movingObjectPosition);
        }
    }
}
