package mtr.entity;

import mtr.mappings.EntityMapper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class EntityBase extends EntityMapper {

	private int clientInterpolationSteps;
	private double clientX;
	private double clientY;
	private double clientZ;
	private double speedX;
	private double speedY;
	private double speedZ;

	public EntityBase(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	public void lerpMotion(Vec3 movement) {
		this.speedX = movement.x();
		this.speedY = movement.y();
		this.speedZ = movement.z();
		setDeltaMovement(speedX, speedY, speedZ);
	}

	protected final void setClientPosition() {
		if (clientInterpolationSteps > 0) {
			final double x = getX() + (clientX - getX()) / clientInterpolationSteps;
			final double y = getY() + (clientY - getY()) / clientInterpolationSteps;
			final double z = getZ() + (clientZ - getZ()) / clientInterpolationSteps;
			--clientInterpolationSteps;
			setPos(x, y, z);
		} else {
			reapplyPosition();
		}
	}
}
