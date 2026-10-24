package com.fib.fib.init.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 钩爪的几何辅助工具：方块表面钳制、附近方块查找、锚点有效性判定。
 *
 * 全部为无状态的静态方法，仅依赖传入的 {@link Level} 与坐标，便于单测与复用。
 */
public final class HookGeometry {

    private HookGeometry() {}

    /**
     * 把锚点钳制到目标方块的表面：
     *  - 先取方块 AABB 内最近点；
     *  - 若点仍落在方块内部，则沿最短轴推出到表面。
     * 这样即使命中点/兜底坐标在空气中，锚点也会贴住方块，不会“悬空”。
     */
    public static Vec3 clampToBlockSurface(Level level, Vec3 point, BlockPos pos) {
        var shape = level.getBlockState(pos).getShape(level, pos);
        if (shape.isEmpty()) {
            // 非实心方块：退化为方块中心
            return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }
        var box = shape.bounds().move(pos);
        double x = Math.min(Math.max(point.x, box.minX), box.maxX);
        double y = Math.min(Math.max(point.y, box.minY), box.maxY);
        double z = Math.min(Math.max(point.z, box.minZ), box.maxZ);

        // 若点落在内部（三个轴都严格在内），沿“离哪个面最近”推出 0.05
        boolean insideX = x > box.minX + 1.0E-4 && x < box.maxX - 1.0E-4;
        boolean insideY = y > box.minY + 1.0E-4 && y < box.maxY - 1.0E-4;
        boolean insideZ = z > box.minZ + 1.0E-4 && z < box.maxZ - 1.0E-4;
        if (insideX && insideY && insideZ) {
            double dx = Math.min(x - box.minX, box.maxX - x);
            double dy = Math.min(y - box.minY, box.maxY - y);
            double dz = Math.min(z - box.minZ, box.maxZ - z);
            final double push = 0.05;
            if (dx <= dy && dx <= dz) {
                x = (x - box.minX < box.maxX - x) ? box.minX - push : box.maxX + push;
            } else if (dy <= dx && dy <= dz) {
                y = (y - box.minY < box.maxY - y) ? box.minY - push : box.maxY + push;
            } else {
                z = (z - box.minZ < box.maxZ - z) ? box.minZ - push : box.maxZ + push;
            }
        }
        return new Vec3(x, y, z);
    }

    /**
     * 在指定位置附近（3x3x3，优先向下）寻找一个「有碰撞形状」的方块，
     * 返回离该位置最近的一个（无则 null）。
     */
    @Nullable
    public static BlockPos findSolidBlockNearby(Level level, Vec3 at) {
        BlockPos center = BlockPos.containing(at);
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dy = 1; dy >= -1; dy--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState st = level.getBlockState(p);
                    if (st.isAir() || st.getCollisionShape(level, p).isEmpty()) {
                        continue;
                    }
                    double d = at.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
                    if (d < bestDist) {
                        bestDist = d;
                        best = p;
                    }
                }
            }
        }
        return best;
    }

    /** 指定锚点附近（3x3x3）是否存在有碰撞形状的方块，用于检测“悬空钩住”。 */
    public static boolean hasSolidBlockNear(Level level, Vec3 anchor) {
        BlockPos base = BlockPos.containing(anchor);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = base.offset(dx, dy, dz);
                    BlockState st = level.getBlockState(p);
                    if (!st.isAir() && !st.getCollisionShape(level, p).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
